package io.github.game2d.client;

import io.github.game2d.model.Team;
import io.github.game2d.protocol.LobbyPlayerState;
import io.github.game2d.protocol.LobbyState;
import io.github.game2d.server.Lobby;
import java.util.ArrayList;
import java.util.List;

public final class LobbyView {
    private static final String LEADER_MARK = "leader";
    private static final String MY_MARK = "you";

    private final LobbyState state;
    private final int myId;

    public LobbyView(LobbyState state, int myId) {
        this.state = state;
        this.myId = myId;
    }

    public static String mapLabel(String map) {
        String name = map.substring(map.lastIndexOf('/') + 1);
        int extension = name.lastIndexOf('.');
        return extension < 0 ? name : name.substring(0, extension);
    }

    public List<String> playerLabels(Team team) {
        return playersOf(team).stream().map(this::label).toList();
    }

    public int count(Team team) {
        return playersOf(team).size();
    }

    public boolean isLeader() {
        return state.leaderId() == myId;
    }

    public boolean canJoin(Team team) {
        LobbyPlayerState me = me();
        return me != null && me.team() != team && count(team) < Lobby.MAX_TEAM_SIZE;
    }

    public boolean canChooseMap() {
        return isLeader();
    }

    public boolean canStart() {
        return isLeader() && state.canStart();
    }

    public String status() {
        if (!state.canStart()) {
            return "Each team needs at least one player";
        }
        return isLeader() ? "You can start the game" : "Waiting for the leader…";
    }

    private String label(LobbyPlayerState player) {
        List<String> marks = new ArrayList<>();
        if (player.id() == state.leaderId()) {
            marks.add(LEADER_MARK);
        }
        if (player.id() == myId) {
            marks.add(MY_MARK);
        }
        return marks.isEmpty() ? player.name() : player.name() + " (" + String.join(", ", marks) + ")";
    }

    private List<LobbyPlayerState> playersOf(Team team) {
        return state.players().stream().filter(player -> player.team() == team).toList();
    }

    private LobbyPlayerState me() {
        return state.players().stream().filter(player -> player.id() == myId).findFirst().orElse(null);
    }
}
