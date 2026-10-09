package io.github.game2d.net.kryo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import io.github.game2d.model.arena.ArenaLoader;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.JoinAccepted;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;
import io.github.game2d.server.GameServer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Test d'intégration : un vrai client KryoNet parle à notre NetworkServer.
// Le GameServer n'est pas démarré en thread : le test appelle tick() lui-même.
class NetworkServerTest {
    private static final String MAP = """
        ######
        #1..4#
        #....#
        ######
        """;
    private static final int TCP_PORT = 54755;
    private static final int UDP_PORT = 54756;
    private static final long TIMEOUT_MS = 3000;
    private static final float FIRST_SPAWN_X = 1.5f;

    private GameServer gameServer;
    private NetworkServer networkServer;
    private final List<TestClient> clients = new ArrayList<>();

    private static class TestClient {
        final Client client = new Client();
        final AtomicReference<JoinAccepted> joined = new AtomicReference<>();
        final AtomicReference<WorldSnapshot> snapshot = new AtomicReference<>();
        private volatile boolean running = true;
        private final Thread thread;

        TestClient() {
            NetworkRegistry.register(client.getKryo());
            client.addListener(new Listener() {
                @Override
                public void received(Connection connection, Object object) {
                    if (object instanceof JoinAccepted accepted) {
                        joined.set(accepted);
                    } else if (object instanceof WorldSnapshot received) {
                        snapshot.set(received);
                    }
                }
            });
            thread = new Thread(() -> {
                while (running) {
                    try {
                        client.update(50);
                    } catch (IOException e) {
                        return;
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
        }

        void connect() throws IOException {
            client.connect(3000, "localhost", TCP_PORT, UDP_PORT);
        }

        void close() throws InterruptedException, IOException {
            running = false;
            thread.join();
            client.close();
            client.dispose();
        }
    }

    @BeforeEach
    void startServer() throws IOException {
        gameServer = new GameServer(ArenaLoader.parse(MAP));
        networkServer = new NetworkServer(gameServer);
        networkServer.start(TCP_PORT, UDP_PORT);
    }

    @AfterEach
    void stopEverything() throws InterruptedException, IOException {
        for (TestClient client : clients) {
            client.close();
        }
        networkServer.stop();
    }

    private TestClient newConnectedClient() throws IOException {
        TestClient client = new TestClient();
        clients.add(client);
        client.connect();
        return client;
    }

    private static void awaitTrue(String description, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting for: " + description);
            }
            Thread.sleep(20);
        }
    }

    private static BrawlerState brawler(WorldSnapshot snapshot, int id) {
        return snapshot.brawlers().stream().filter(b -> b.id() == id).findFirst().orElse(null);
    }

    @Test
    void connectingClientIsToldWhichBrawlerIsItsOwn() throws Exception {
        TestClient client = newConnectedClient();

        awaitTrue("JoinAccepted", () -> client.joined.get() != null);
        int id = client.joined.get().brawlerId();

        awaitTrue("snapshot containing the brawler", () -> {
            gameServer.tick();
            WorldSnapshot snapshot = client.snapshot.get();
            return snapshot != null && brawler(snapshot, id) != null;
        });
        assertEquals(FIRST_SPAWN_X, brawler(client.snapshot.get(), id).x());
    }

    @Test
    void inputSentByTheClientMovesItsBrawler() throws Exception {
        TestClient client = newConnectedClient();
        awaitTrue("JoinAccepted", () -> client.joined.get() != null);
        int id = client.joined.get().brawlerId();

        client.client.sendTCP(new PlayerInput(1, 1, 0, 1, 0, false));

        awaitTrue("brawler moved to the right", () -> {
            gameServer.tick();
            WorldSnapshot snapshot = client.snapshot.get();
            return snapshot != null && brawler(snapshot, id) != null && brawler(snapshot, id).x() > FIRST_SPAWN_X;
        });
    }

    @Test
    void twoClientsGetDifferentBrawlersAndBothSeeThem() throws Exception {
        TestClient first = newConnectedClient();
        awaitTrue("first JoinAccepted", () -> first.joined.get() != null);
        TestClient second = newConnectedClient();
        awaitTrue("second JoinAccepted", () -> second.joined.get() != null);

        assertNotEquals(first.joined.get().brawlerId(), second.joined.get().brawlerId());
        awaitTrue("both clients see two brawlers", () -> {
            gameServer.tick();
            WorldSnapshot a = first.snapshot.get();
            WorldSnapshot b = second.snapshot.get();
            return a != null && b != null && a.brawlers().size() == 2 && b.brawlers().size() == 2;
        });
    }

    @Test
    void inputOfOneClientDoesNotMoveTheOther() throws Exception {
        TestClient first = newConnectedClient();
        awaitTrue("first JoinAccepted", () -> first.joined.get() != null);
        TestClient second = newConnectedClient();
        awaitTrue("second JoinAccepted", () -> second.joined.get() != null);
        int firstId = first.joined.get().brawlerId();
        int secondId = second.joined.get().brawlerId();

        first.client.sendTCP(new PlayerInput(1, 0, 1, 0, 1, false));

        awaitTrue("first brawler moved up", () -> {
            gameServer.tick();
            WorldSnapshot snapshot = second.snapshot.get();
            return snapshot != null && brawler(snapshot, firstId) != null && brawler(snapshot, firstId).y() > 2.5f;
        });
        WorldSnapshot snapshot = second.snapshot.get();
        assertNotNull(brawler(snapshot, secondId));
        assertEquals(2.5f, brawler(snapshot, secondId).y());
        assertTrue(brawler(snapshot, firstId).y() > 2.5f);
    }
}
