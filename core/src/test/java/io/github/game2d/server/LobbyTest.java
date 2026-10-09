package io.github.game2d.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.model.Team;
import java.util.List;
import org.junit.jupiter.api.Test;

class LobbyTest {
    private static final List<String> MAPS = List.of("maps/a.txt", "maps/b.txt");

    private final Lobby lobby = new Lobby(MAPS, "maps/a.txt");

    private static void assertRefused(LobbyException.Reason reason, Runnable action) {
        LobbyException error = assertThrows(LobbyException.class, action::run);
        assertEquals(reason, error.getReason());
    }

    @Test
    void firstPlayerIsTheLeader() {
        lobby.join(1, "Louis");
        lobby.join(2, "Lilian");

        assertEquals(1, lobby.leaderId());
        assertTrue(lobby.isLeader(1));
        assertFalse(lobby.isLeader(2));
    }

    @Test
    void newPlayerJoinsTheSmallestTeam() {
        assertEquals(Team.BLUE, lobby.join(1, "Louis").team());
        assertEquals(Team.RED, lobby.join(2, "Lilian").team());
        assertEquals(Team.BLUE, lobby.join(3, "Tom").team());
    }

    @Test
    void nameIsStrippedOfSurroundingSpaces() {
        assertEquals("Louis", lobby.join(1, "  Louis ").name());
    }

    @Test
    void acceptsAccentsDigitsUnderscoresAndDashes() {
        assertEquals("Élodie_42-x", lobby.join(1, "Élodie_42-x").name());
    }

    @Test
    void rejectsEmptyName() {
        assertRefused(LobbyException.Reason.INVALID_NAME, () -> lobby.join(1, "   "));
        assertRefused(LobbyException.Reason.INVALID_NAME, () -> lobby.join(1, null));
    }

    @Test
    void rejectsNameLongerThanSixteenCharacters() {
        assertEquals("a".repeat(16), lobby.join(1, "a".repeat(16)).name());
        assertRefused(LobbyException.Reason.INVALID_NAME, () -> lobby.join(2, "b".repeat(17)));
    }

    @Test
    void rejectsSpacesAndSymbolsInsideTheName() {
        assertRefused(LobbyException.Reason.INVALID_NAME, () -> lobby.join(1, "Louis P"));
        assertRefused(LobbyException.Reason.INVALID_NAME, () -> lobby.join(1, "Louis!"));
    }

    @Test
    void rejectsNameAlreadyTakenWhateverTheCase() {
        lobby.join(1, "Louis");

        assertRefused(LobbyException.Reason.NAME_TAKEN, () -> lobby.join(2, "LOUIS"));
    }

    @Test
    void rejectsSeventhPlayer() {
        for (int id = 1; id <= 6; id++) {
            lobby.join(id, "Player" + id);
        }

        assertRefused(LobbyException.Reason.LOBBY_FULL, () -> lobby.join(7, "Player7"));
    }

    @Test
    void nextPlayerBecomesLeaderWhenTheLeaderLeaves() {
        lobby.join(1, "Louis");
        lobby.join(2, "Lilian");
        lobby.join(3, "Tom");

        lobby.leave(1);

        assertEquals(2, lobby.leaderId());
    }

    @Test
    void leaderStaysWhenAnotherPlayerLeaves() {
        lobby.join(1, "Louis");
        lobby.join(2, "Lilian");

        lobby.leave(2);

        assertEquals(1, lobby.leaderId());
        assertEquals(1, lobby.players().size());
    }

    @Test
    void emptyLobbyHasNoLeader() {
        lobby.join(1, "Louis");

        lobby.leave(1);

        assertEquals(Lobby.NO_LEADER, lobby.leaderId());
        assertFalse(lobby.isLeader(Lobby.NO_LEADER));
    }

    @Test
    void playerCanChangeTeam() {
        lobby.join(1, "Louis");

        lobby.chooseTeam(1, Team.RED);

        assertEquals(Team.RED, lobby.players().get(0).team());
    }

    @Test
    void choosingTheSameTeamChangesNothing() {
        lobby.join(1, "Louis");

        lobby.chooseTeam(1, Team.BLUE);

        assertEquals(Team.BLUE, lobby.players().get(0).team());
    }

    @Test
    void rejectsJoiningAFullTeam() {
        for (int id = 1; id <= 4; id++) {
            lobby.join(id, "Player" + id);
        }
        lobby.chooseTeam(2, Team.BLUE);

        assertRefused(LobbyException.Reason.TEAM_FULL, () -> lobby.chooseTeam(4, Team.BLUE));
    }

    @Test
    void playersKeepTheirJoiningOrder() {
        lobby.join(1, "Louis");
        lobby.join(2, "Lilian");

        assertEquals(List.of("Louis", "Lilian"), lobby.players().stream().map(LobbyPlayer::name).toList());
    }

    @Test
    void playersCannotBeModifiedFromOutside() {
        lobby.join(1, "Louis");

        assertThrows(UnsupportedOperationException.class,
            () -> lobby.players().add(new LobbyPlayer(2, "Intrus", Team.RED)));
    }

    @Test
    void rejectsADefaultMapThatIsNotInTheList() {
        assertThrows(IllegalArgumentException.class, () -> new Lobby(MAPS, "maps/missing.txt"));
    }

    @Test
    void startsWithTheDefaultMap() {
        assertEquals("maps/a.txt", lobby.map());
        assertEquals(MAPS, lobby.maps());
    }

    @Test
    void leaderChoosesTheMap() {
        lobby.join(1, "Louis");

        lobby.chooseMap(1, "maps/b.txt");

        assertEquals("maps/b.txt", lobby.map());
    }

    @Test
    void rejectsMapChoiceFromAPlayerWhoIsNotTheLeader() {
        lobby.join(1, "Louis");
        lobby.join(2, "Lilian");

        assertRefused(LobbyException.Reason.NOT_LEADER, () -> lobby.chooseMap(2, "maps/b.txt"));
        assertEquals("maps/a.txt", lobby.map());
    }

    @Test
    void rejectsAnUnknownMap() {
        lobby.join(1, "Louis");

        assertRefused(LobbyException.Reason.UNKNOWN_MAP, () -> lobby.chooseMap(1, "maps/missing.txt"));
    }

    @Test
    void cannotStartAlone() {
        lobby.join(1, "Louis");

        assertFalse(lobby.canStart());
    }

    @Test
    void canStartOneAgainstOne() {
        lobby.join(1, "Louis");
        lobby.join(2, "Lilian");

        assertTrue(lobby.canStart());
    }

    @Test
    void canStartTwoAgainstOne() {
        lobby.join(1, "Louis");
        lobby.join(2, "Lilian");
        lobby.join(3, "Tom");

        assertTrue(lobby.canStart());
    }

    @Test
    void canStartThreeAgainstThree() {
        for (int id = 1; id <= 6; id++) {
            lobby.join(id, "Player" + id);
        }

        assertTrue(lobby.canStart());
    }

    @Test
    void cannotStartWithAnEmptyTeam() {
        lobby.join(1, "Louis");
        lobby.join(2, "Lilian");
        lobby.chooseTeam(2, Team.BLUE);

        assertFalse(lobby.canStart());
    }
}
