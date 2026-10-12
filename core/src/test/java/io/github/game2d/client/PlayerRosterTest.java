package io.github.game2d.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.model.Team;
import io.github.game2d.protocol.LobbyPlayerState;
import io.github.game2d.protocol.LobbyState;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerRosterTest {
    private static final LobbyState LOBBY = new LobbyState(
        List.of(
            new LobbyPlayerState(1, "Louis", Team.BLUE),
            new LobbyPlayerState(2, "Lilian", Team.RED),
            new LobbyPlayerState(3, "Tom", Team.BLUE)),
        1, "maps/arena2.txt", List.of("maps/arena2.txt"), true, true);

    @Test
    void findsTheNameOfEachPlayer() {
        PlayerRoster roster = PlayerRoster.from(LOBBY, 1);

        assertEquals("Louis", roster.nameOf(1));
        assertEquals("Lilian", roster.nameOf(2));
    }

    @Test
    void unknownIdGivesAnEmptyName() {
        assertEquals("", PlayerRoster.from(LOBBY, 1).nameOf(9));
    }

    @Test
    void playersOfMyTeamAreAllies() {
        PlayerRoster roster = PlayerRoster.from(LOBBY, 1);

        assertTrue(roster.isAlly(1));
        assertTrue(roster.isAlly(3));
        assertFalse(roster.isAlly(2));
        assertFalse(roster.isAlly(9));
    }

    @Test
    void alliesDependOnWhoIsLooking() {
        PlayerRoster roster = PlayerRoster.from(LOBBY, 2);

        assertTrue(roster.isAlly(2));
        assertFalse(roster.isAlly(1));
        assertFalse(roster.isAlly(3));
    }

    @Test
    void withoutLobbyOnlyMyselfIsAnAlly() {
        PlayerRoster roster = PlayerRoster.from(null, 1);

        assertEquals("", roster.nameOf(1));
        assertTrue(roster.isAlly(1));
        assertFalse(roster.isAlly(2));
    }
}
