package io.github.game2d.net.kryo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.model.match.MatchPhase;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.MatchState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.ProjectileState;
import io.github.game2d.protocol.WorldSnapshot;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

// Test jetable : vérifie que KryoNet transporte nos records (PlayerInput, WorldSnapshot).
// On n'appelle ni start() ni stop() : nos propres threads appellent update(), et la
// connexion est fermée une fois ces threads terminés, pour éviter le blocage de Client.stop().
class KryoNetSpikeTest {
    private static final int TCP_PORT = 54655;
    private static final int UDP_PORT = 54656;
    private static final long TIMEOUT_MS = 3000;
    private static final int REPEATS = 10;

    private final Server server = new Server();
    private final Client client = new Client();
    private volatile boolean running;
    private Thread serverThread;
    private Thread clientThread;

    private static void register(Kryo kryo) {
        kryo.register(PlayerInput.class);
        kryo.register(WorldSnapshot.class);
        kryo.register(BrawlerState.class);
        kryo.register(ProjectileState.class);
        kryo.register(BrawlerType.class);
        kryo.register(ArrayList.class);
        kryo.register(MatchState.class);
        kryo.register(MatchPhase.class);
    }

    private static <T> T await(AtomicReference<T> box) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (box.get() == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        return box.get();
    }

    @FunctionalInterface
    private interface Update {
        void run() throws IOException;
    }

    private Thread startUpdating(String name, Update update) {
        Thread thread = new Thread(() -> {
            while (running) {
                try {
                    update.run();
                } catch (IOException e) {
                    return;
                }
            }
        }, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    @BeforeEach
    void connect() throws IOException {
        register(server.getKryo());
        register(client.getKryo());
        server.bind(TCP_PORT, UDP_PORT);
        running = true;
        serverThread = startUpdating("spike-server", () -> server.update(50));
        clientThread = startUpdating("spike-client", () -> client.update(50));
        client.connect(3000, "localhost", TCP_PORT, UDP_PORT);
    }

    @AfterEach
    void disconnect() throws InterruptedException, IOException {
        running = false;
        serverThread.join();
        clientThread.join();
        client.close();
        server.close();
        client.dispose();
        server.dispose();
    }

    @RepeatedTest(REPEATS)
    void inputTravelsFromClientToServer() throws InterruptedException {
        AtomicReference<PlayerInput> received = new AtomicReference<>();
        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof PlayerInput input) {
                    received.set(input);
                }
            }
        });
        PlayerInput sent = new PlayerInput(7, 1, 0, 0.6f, 0.8f, true);

        client.sendTCP(sent);

        assertEquals(sent, await(received));
    }

    @RepeatedTest(REPEATS)
    void snapshotTravelsFromServerToClientOverTcp() throws InterruptedException {
        AtomicReference<WorldSnapshot> received = new AtomicReference<>();
        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof WorldSnapshot snapshot) {
                    received.set(snapshot);
                }
            }
        });
        WorldSnapshot sent = sampleSnapshot();

        server.sendToAllTCP(sent);

        assertNotNull(await(received));
        assertEquals(sent, received.get());
    }

    @RepeatedTest(REPEATS)
    void snapshotTravelsFromServerToClientOverUdp() throws InterruptedException {
        AtomicReference<WorldSnapshot> received = new AtomicReference<>();
        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof WorldSnapshot snapshot) {
                    received.set(snapshot);
                }
            }
        });
        WorldSnapshot sent = sampleSnapshot();

        server.sendToAllUDP(sent);

        assertNotNull(await(received));
        assertEquals(sent, received.get());
    }

    private static WorldSnapshot sampleSnapshot() {
        // des ArrayList, comme celles que construit SnapshotBuilder.build()
        List<BrawlerState> brawlers = new ArrayList<>(List.of(
            new BrawlerState(1, 1.5f, 2.5f, 0.25f, 120, 120, 3, 3, 0f, BrawlerType.SHELLY, 7),
            new BrawlerState(2, 4.5f, 2.5f, 0.25f, 80, 100, 1, 3, 0.4f, BrawlerType.COLT, 5)));
        List<ProjectileState> projectiles = new ArrayList<>(List.of(
            new ProjectileState(3, 2f, 2.5f, 0.05f),
            new ProjectileState(4, 2.1f, 2.6f, 0.05f)));
        return new WorldSnapshot(42, brawlers, projectiles, null);
    }
}
