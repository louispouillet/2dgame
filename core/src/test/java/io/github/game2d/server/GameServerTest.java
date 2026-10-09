package io.github.game2d.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        #1..4#
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

        channel.sendInput(new PlayerInput(1, 1, 0, 1, 0, false));
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

        first.sendInput(new PlayerInput(1, 0, 1, 0, 1, false));
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
    void snapshotTickIncreasesByOneAtEachTick() {
        LocalChannel channel = new LocalChannel();
        server.join(channel);

        server.tick();
        int first = channel.latestSnapshot().tick();
        server.tick();
        int second = channel.latestSnapshot().tick();

        assertEquals(first + 1, second);
    }

    @Test
    void snapshotAcknowledgesTheLastProcessedInput() {
        LocalChannel channel = new LocalChannel();
        int id = server.join(channel);

        channel.sendInput(new PlayerInput(5, 1, 0, 1, 0, false));
        server.tick();

        assertEquals(5, brawlerOf(channel, id).lastProcessedInput());
    }

    @Test
    void eachBrawlerIsAcknowledgedForItsOwnInputs() {
        LocalChannel first = new LocalChannel();
        LocalChannel second = new LocalChannel();
        int firstId = server.join(first);
        int secondId = server.join(second);

        first.sendInput(new PlayerInput(3, 0, 0, 1, 0, false));
        second.sendInput(new PlayerInput(8, 0, 0, 1, 0, false));
        server.tick();

        assertEquals(3, brawlerOf(first, firstId).lastProcessedInput());
        assertEquals(8, brawlerOf(first, secondId).lastProcessedInput());
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

    @Test
    void playersAlternateBetweenBlueAndRedSpawns() {
        GameServer teams = new GameServer(ArenaLoader.parse("""
            ########
            #1.2.4.#
            #3.5.6.#
            ########
            """));
        LocalChannel first = new LocalChannel();
        LocalChannel second = new LocalChannel();
        LocalChannel third = new LocalChannel();

        int firstId = teams.join(first);
        int secondId = teams.join(second);
        int thirdId = teams.join(third);
        teams.tick();

        assertEquals(1.5f, brawlerOf(first, firstId).x());
        assertEquals(5.5f, brawlerOf(first, secondId).x());
        assertEquals(3.5f, brawlerOf(first, thirdId).x());
    }

    @Test
    void seventhPlayerIsRefused() {
        GameServer full = new GameServer(ArenaLoader.loadResource(ArenaLoader.DEFAULT_MAP));
        for (int i = 0; i < 6; i++) {
            full.join(new LocalChannel());
        }

        assertThrows(IllegalStateException.class, () -> full.join(new LocalChannel()));
    }
}
