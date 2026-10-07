package io.github.game2d.net.kryo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.game2d.model.arena.ArenaLoader;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;
import io.github.game2d.server.GameServer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Test de bout en bout : notre NetworkClient parle à notre NetworkServer.
// Le GameServer n'est pas démarré en thread : le test appelle tick() lui-même.
class NetworkClientTest {
    private static final String MAP = """
        ######
        #1..2#
        #....#
        ######
        """;
    private static final String HOST = "localhost";
    private static final int TCP_PORT = 54855;
    private static final int UDP_PORT = 54856;
    private static final long TIMEOUT_MS = 3000;
    private static final float FIRST_SPAWN_X = 1.5f;

    private GameServer gameServer;
    private NetworkServer networkServer;
    private final List<NetworkClient> remotes = new ArrayList<>();

    @BeforeEach
    void startServer() throws IOException {
        gameServer = new GameServer(ArenaLoader.parse(MAP));
        networkServer = new NetworkServer(gameServer);
        networkServer.start(TCP_PORT, UDP_PORT);
    }

    @AfterEach
    void stopEverything() {
        for (NetworkClient remote : remotes) {
            remote.stop();
        }
        networkServer.stop();
    }

    private NetworkClient connectedRemote() throws IOException {
        NetworkClient remote = new NetworkClient();
        remotes.add(remote);
        remote.connect(HOST, TCP_PORT, UDP_PORT);
        return remote;
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
        if (snapshot == null) {
            return null;
        }
        return snapshot.brawlers().stream().filter(b -> b.id() == id).findFirst().orElse(null);
    }

    @Test
    void idIsUnknownUntilTheServerAnswers() {
        NetworkClient remote = new NetworkClient();

        assertEquals(-1, remote.channel().getBrawlerId());
    }

    @Test
    void remoteLearnsItsBrawlerId() throws Exception {
        NetworkClient remote = connectedRemote();
        NetworkServerChannel channel = remote.channel();

        awaitTrue("brawler id", () -> channel.getBrawlerId() != -1);

        awaitTrue("snapshot with its own brawler", () -> {
            gameServer.tick();
            return brawler(channel.latestSnapshot(), channel.getBrawlerId()) != null;
        });
        assertEquals(FIRST_SPAWN_X, brawler(channel.latestSnapshot(), channel.getBrawlerId()).x());
    }

    @Test
    void inputSentWithSendInputMovesTheBrawler() throws Exception {
        NetworkServerChannel channel = connectedRemote().channel();
        awaitTrue("brawler id", () -> channel.getBrawlerId() != -1);

        channel.sendInput(new PlayerInput(1, 1, 0, 1, 0, false));

        awaitTrue("brawler moved to the right", () -> {
            gameServer.tick();
            BrawlerState state = brawler(channel.latestSnapshot(), channel.getBrawlerId());
            return state != null && state.x() > FIRST_SPAWN_X;
        });
    }

    @Test
    void twoRemotesControlTheirOwnBrawler() throws Exception {
        NetworkServerChannel first = connectedRemote().channel();
        awaitTrue("first id", () -> first.getBrawlerId() != -1);
        NetworkServerChannel second = connectedRemote().channel();
        awaitTrue("second id", () -> second.getBrawlerId() != -1);
        assertNotEquals(first.getBrawlerId(), second.getBrawlerId());

        first.sendInput(new PlayerInput(1, 0, 1, 0, 1, false));

        awaitTrue("first brawler moved up, seen by the second remote", () -> {
            gameServer.tick();
            BrawlerState moved = brawler(second.latestSnapshot(), first.getBrawlerId());
            return moved != null && moved.y() > 2.5f;
        });
        assertEquals(2.5f, brawler(second.latestSnapshot(), second.getBrawlerId()).y());
    }

    @Test
    void connectingToAnUnreachableServerFails() {
        NetworkClient remote = new NetworkClient();

        assertThrows(IOException.class, () -> remote.connect(HOST, 54999, 55000));
    }
}
