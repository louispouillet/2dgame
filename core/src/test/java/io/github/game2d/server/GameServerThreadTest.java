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
        // un tick déplace le brawler d'une distance fixe : on s'en sert pour compter les ticks
        GameServer calibration = newServer();
        LocalChannel calibrationChannel = new LocalChannel();
        joinAndStart(calibration, calibrationChannel);
        calibration.tick();
        float startX = firstBrawlerX(calibrationChannel);
        calibrationChannel.sendInput(MOVE_RIGHT);
        calibration.tick();
        float distancePerTick = firstBrawlerX(calibrationChannel) - startX;

        GameServer server = newServer();
        LocalChannel channel = new LocalChannel();
        joinAndStart(server, channel);
        channel.sendInput(MOVE_RIGHT);

        long begin = System.nanoTime();
        server.start();
        Thread.sleep(1000);
        server.stop();
        float elapsedSeconds = (System.nanoTime() - begin) / 1_000_000_000f;

        float ticks = (firstBrawlerX(channel) - startX) / distancePerTick;
        float expected = TICK_RATE * elapsedSeconds;
        assertTrue(ticks > expected * 0.5f, "too slow: " + ticks + " ticks, expected about " + expected);
        assertTrue(ticks < expected * 1.5f, "too fast: " + ticks + " ticks, expected about " + expected);
    }
}
