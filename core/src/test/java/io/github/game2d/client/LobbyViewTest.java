package io.github.game2d.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.model.Team;
import io.github.game2d.protocol.LobbyPlayerState;
import io.github.game2d.protocol.LobbyState;
import java.util.List;
import org.junit.jupiter.api.Test;

class LobbyViewTest {
    private static final String MAP = "maps/arena2.txt";

    private static LobbyState lobby(int leaderId, boolean canStart, LobbyPlayerState... players) {
        return new LobbyState(List.of(players), leaderId, MAP, List.of(MAP), canStart, false);
    }

    private static LobbyPlayerState blue(int id, String name) {
        return new LobbyPlayerState(id, name, Team.BLUE);
    }

    private static LobbyPlayerState red(int id, String name) {
        return new LobbyPlayerState(id, name, Team.RED);
    }

    @Test
    void mapLabelDropsTheFolderAndTheExtension() {
        assertEquals("arena2", LobbyView.mapLabel("maps/arena2.txt"));
        assertEquals("arena", LobbyView.mapLabel("arena"));
    }

    @Test
    void labelsMarkTheLeaderAndThePlayerThemself() {
        LobbyView view = new LobbyView(lobby(1, true, blue(1, "Louis"), red(2, "Lilian"), blue(3, "Tom")), 3);

        assertEquals(List.of("Louis (leader)", "Tom (you)"), view.playerLabels(Team.BLUE));
        assertEquals(List.of("Lilian"), view.playerLabels(Team.RED));
        assertEquals(2, view.count(Team.BLUE));
    }

    @Test
    void leaderSeesBothMarksOnTheirName() {
        LobbyView view = new LobbyView(lobby(1, true, blue(1, "Louis")), 1);

        assertEquals(List.of("Louis (leader, you)"), view.playerLabels(Team.BLUE));
    }

    @Test
    void playerCanJoinTheOtherTeamOnly() {
        LobbyView view = new LobbyView(lobby(1, true, blue(1, "Louis"), red(2, "Lilian")), 1);

        assertFalse(view.canJoin(Team.BLUE));
        assertTrue(view.canJoin(Team.RED));
    }

    @Test
    void playerCannotJoinAFullTeam() {
        LobbyView view = new LobbyView(lobby(1, true,
            blue(1, "Louis"), red(2, "Lilian"), red(3, "Tom"), red(4, "Lea")), 1);

        assertFalse(view.canJoin(Team.RED));
    }

    @Test
    void unknownPlayerCannotJoinAnyTeam() {
        LobbyView view = new LobbyView(lobby(1, true, blue(1, "Louis")), 9);

        assertFalse(view.canJoin(Team.RED));
    }

    @Test
    void onlyTheLeaderChoosesTheMapAndStarts() {
        LobbyState state = lobby(1, true, blue(1, "Louis"), red(2, "Lilian"));

        assertTrue(new LobbyView(state, 1).canChooseMap());
        assertTrue(new LobbyView(state, 1).canStart());
        assertFalse(new LobbyView(state, 2).canChooseMap());
        assertFalse(new LobbyView(state, 2).canStart());
    }

    @Test
    void leaderCannotStartWhenTheTeamsAreNotReady() {
        LobbyView view = new LobbyView(lobby(1, false, blue(1, "Louis")), 1);

        assertFalse(view.canStart());
        assertEquals("Each team needs at least one player", view.status());
    }

    @Test
    void statusTellsWhoCanStart() {
        LobbyState state = lobby(1, true, blue(1, "Louis"), red(2, "Lilian"));

        assertEquals("You can start the game", new LobbyView(state, 1).status());
        assertEquals("Waiting for the leader…", new LobbyView(state, 2).status());
    }
}
