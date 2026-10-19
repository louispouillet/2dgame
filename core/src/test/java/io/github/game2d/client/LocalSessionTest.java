package io.github.game2d.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.game2d.model.Team;
import io.github.game2d.model.arena.ArenaLoader;
import io.github.game2d.model.arena.TestArenas;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.protocol.LobbyPlayerState;
import io.github.game2d.protocol.LobbyState;
import io.github.game2d.protocol.RefusalReason;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LocalSessionTest {
    private static final long TIMEOUT_MS = 2000;

    private final LocalSession session = new LocalSession(TestArenas.open(20, 12));

    @AfterEach
    void closeSession() {
        session.close();
    }

    private LobbyPlayerState player(String name) {
        return session.channel().latestLobby().players().stream()
            .filter(player -> player.name().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void playerIdIsUnknownBeforeJoining() {
        assertEquals(-1, session.playerId());
        assertNull(session.rejection());
    }

    @Test
    void joiningAddsThePlayerAndATargetInTheOtherTeam() {
        session.join("Louis");

        LobbyState lobby = session.channel().latestLobby();
        assertNotEquals(-1, session.playerId());
        assertEquals(session.playerId(), lobby.leaderId());
        assertEquals(2, lobby.players().size());
        assertNotEquals(player("Louis").team(), player("Target").team());
    }

    @Test
    void invalidNameIsRejectedWithItsReason() {
        session.join("Louis !");

        assertEquals(RefusalReason.INVALID_NAME, session.rejection());
        assertEquals(-1, session.playerId());
    }

    @Test
    void playerCanChangeTeamBeforeStarting() {
        session.join("Louis");

        session.chooseTeam(Team.RED);

        assertEquals(Team.RED, player("Louis").team());
    }

    @Test
    void playerCanChooseABrawlerBeforeStarting() {
        session.join("Louis");

        session.chooseBrawler(BrawlerType.SHELLY);

        assertEquals(BrawlerType.SHELLY, player("Louis").brawler());
    }

    @Test
    void startingSendsSnapshotsWithThePlayersBrawler() throws InterruptedException {
        session.join("Louis");

        session.start();

        assertTrue(session.channel().latestLobby().started());
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (session.channel().latestSnapshot() == null
                || session.channel().latestSnapshot().findBrawler(session.playerId()) == null) {
            if (System.currentTimeMillis() > deadline) {
                fail("No snapshot with the player's brawler within " + TIMEOUT_MS + " ms");
            }
            Thread.sleep(10);
        }
    }

    @Test
    void refusedRequestsAreIgnored() {
        session.join("Louis");
        session.chooseTeam(Team.RED);

        session.chooseTeam(Team.RED);
        session.chooseMap("maps/missing.txt");

        assertFalse(session.channel().latestLobby().started());
    }

    @Test
    void playerCanBeNamedLikeTheTarget() {
        session.join("cible");

        assertEquals(2, session.channel().latestLobby().players().size());
        assertNull(session.rejection());
    }

    @Test
    void soloMapCanBeLoadedAgainToPlayAnotherMatch() {
        session.join("Louis");

        String map = session.channel().latestLobby().map();

        assertEquals(ArenaLoader.DEFAULT_MAP, map);
        assertNotNull(ArenaLoader.loadResource(map));
    }
}
