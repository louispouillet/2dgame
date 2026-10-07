package io.github.game2d.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.ArenaLoader;
import io.github.game2d.net.LocalChannel;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameServerTest {
    private static final String MAP = """
        ######
        #1..2#
        #....#
        ######
        """;

    private GameServer server;

    @BeforeEach
    void createServer() {
        Arena arena = ArenaLoader.parse(MAP);
        server = new GameServer(arena);
    }

    private static BrawlerState brawlerOf(LocalChannel channel, int id) {
        WorldSnapshot snapshot = channel.latestSnapshot();
        return snapshot.brawlers().stream().filter(b -> b.id() == id).findFirst().orElseThrow();
    }

    @Test
    void joinedBrawlerAppearsAtItsSpawnPointAfterATick() {
        LocalChannel channel = new LocalChannel();

        int id = server.join(channel);
        server.tick();

        BrawlerState state = brawlerOf(channel, id);
        assertEquals(1.5f, state.x());
        assertEquals(2.5f, state.y());
    }

    @Test
    void eachPlayerGetsItsOwnBrawlerAtItsOwnSpawn() {
        LocalChannel first = new LocalChannel();
        LocalChannel second = new LocalChannel();

        int firstId = server.join(first);
        int secondId = server.join(second);
        server.tick();

        assertNotEquals(firstId, secondId);
        assertEquals(2, first.latestSnapshot().brawlers().size());
        assertEquals(4.5f, brawlerOf(first, secondId).x());
    }

    @Test
    void inputMovesTheBrawler() {
        LocalChannel channel = new LocalChannel();
        int id = server.join(channel);

        channel.sendInput(new PlayerInput(1, 0, 1, 0, false));
        server.tick();

        assertTrue(brawlerOf(channel, id).x() > 1.5f);
    }

    @Test
    void playerWithoutInputStaysInPlace() {
        LocalChannel channel = new LocalChannel();
        int id = server.join(channel);

        server.tick();
        server.tick();

        assertEquals(1.5f, brawlerOf(channel, id).x());
        assertEquals(2.5f, brawlerOf(channel, id).y());
    }

    @Test
    void inputOnlyMovesTheBrawlerOfItsSender() {
        LocalChannel first = new LocalChannel();
        LocalChannel second = new LocalChannel();
        int firstId = server.join(first);
        int secondId = server.join(second);

        first.sendInput(new PlayerInput(0, 1, 0, 1, false));
        server.tick();

        assertTrue(brawlerOf(first, firstId).y() > 2.5f);
        assertEquals(2.5f, brawlerOf(first, secondId).y());
    }

    @Test
    void everyClientReceivesTheSameSnapshot() {
        LocalChannel first = new LocalChannel();
        LocalChannel second = new LocalChannel();
        server.join(first);
        server.join(second);

        server.tick();

        assertNotNull(first.latestSnapshot());
        assertEquals(first.latestSnapshot(), second.latestSnapshot());
    }

    @Test
    void leavingPlayerDisappearsFromTheSnapshot() {
        LocalChannel stayer = new LocalChannel();
        LocalChannel leaver = new LocalChannel();
        int stayerId = server.join(stayer);
        int leaverId = server.join(leaver);

        server.leave(leaverId);
        server.tick();

        assertEquals(1, stayer.latestSnapshot().brawlers().size());
        assertEquals(stayerId, stayer.latestSnapshot().brawlers().get(0).id());
    }

    @Test
    void leavingPlayerNoLongerReceivesSnapshots() {
        LocalChannel stayer = new LocalChannel();
        LocalChannel leaver = new LocalChannel();
        server.join(stayer);
        int leaverId = server.join(leaver);

        server.leave(leaverId);
        server.tick();

        assertNull(leaver.latestSnapshot());
    }

    @Test
    void leavingTwiceOrWithAnUnknownIdIsHarmless() {
        LocalChannel channel = new LocalChannel();
        int id = server.join(channel);

        server.leave(id);
        server.leave(id);
        server.leave(999);
        server.tick();

        assertNull(channel.latestSnapshot());
    }

    @Test
    void noSnapshotIsSentBeforeTheFirstTick() {
        LocalChannel channel = new LocalChannel();

        server.join(channel);

        assertNull(channel.latestSnapshot());
    }
}
