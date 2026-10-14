package io.github.game2d.server;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.ArenaLoader;
import io.github.game2d.net.LocalChannel;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameServerThreadTest {
    private static final String MAP = """
        ######
        #1..4#
        #....#
        ######
        """;
    private static final int TICK_RATE = 30;
    private static final long TIMEOUT_MS = 2000;
    private static final PlayerInput MOVE_RIGHT = new PlayerInput(1, 1, 0, 1, 0, false);

    private static GameServer newServer() {
        Arena arena = ArenaLoader.parse(MAP);
        return new GameServer(List.of("test"), "test", map -> arena);
    }

    // Le joueur et un adversaire immobile rejoignent, puis la partie est lancée
    private static void joinAndStart(GameServer server, LocalChannel channel) {
        int id = server.join(channel, "Louis");
        server.join(new LocalChannel(), "Opponent");
        server.requestStart(id);
    }

    private static void waitForSnapshot(LocalChannel channel) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (channel.latestSnapshot() == null) {
            if (System.currentTimeMillis() > deadline) {
                fail("No snapshot received within " + TIMEOUT_MS + " ms");
            }
            Thread.sleep(10);
        }
    }

    private static float firstBrawlerX(LocalChannel channel) {
        return channel.latestSnapshot().brawlers().get(0).x();
    }

    @Test
    void startedServerSendsSnapshotsOnItsOwn() throws InterruptedException {
        GameServer server = newServer();
        LocalChannel channel = new LocalChannel();
        joinAndStart(server, channel);

        server.start();
        try {
            waitForSnapshot(channel);
        } finally {
            server.stop();
        }
    }

    @Test
    void snapshotsStopChangingOnceTheServerIsStopped() throws InterruptedException {
        GameServer server = newServer();
        LocalChannel channel = new LocalChannel();
        joinAndStart(server, channel);
        server.start();
        waitForSnapshot(channel);

        server.stop();
        WorldSnapshot afterStop = channel.latestSnapshot();
        Thread.sleep(200);

        assertSame(afterStop, channel.latestSnapshot());
    }

    @Test
    void serverRunsAtTheConfiguredTickRate() throws InterruptedException {
        GameServer server = newServer();
        LocalChannel channel = new LocalChannel();
        joinAndStart(server, channel);

        server.start();
        try {
            waitForSnapshot(channel);
            int firstTick = channel.latestSnapshot().tick();
            long begin = System.nanoTime();
            Thread.sleep(1000);
            int ticks = channel.latestSnapshot().tick() - firstTick;
            float elapsedSeconds = (System.nanoTime() - begin) / 1_000_000_000f;

            float expected = TICK_RATE * elapsedSeconds;
            assertTrue(ticks > expected * 0.5f, "too slow: " + ticks + " ticks, expected about " + expected);
            assertTrue(ticks < expected * 1.5f, "too fast: " + ticks + " ticks, expected about " + expected);
        } finally {
            server.stop();
        }
    }
}
