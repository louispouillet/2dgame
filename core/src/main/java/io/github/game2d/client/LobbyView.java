package io.github.game2d.client;

import io.github.game2d.model.Team;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.protocol.LobbyPlayerState;
import io.github.game2d.protocol.LobbyState;
import io.github.game2d.server.Lobby;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** What the lobby screen shows and allows, decided from a {@link LobbyState}. Pure Java, no libGDX. */
public final class LobbyView {
    private static final String LEADER_MARK = "leader";
    private static final String MY_MARK = "you";
    private static final String BRAWLER_SEPARATOR = " · ";

    private final LobbyState state;
    private final int myId;

    /**
     * Creates a view.
     *
     * @param state the latest lobby state
     * @param myId  my player id
     */
    public LobbyView(LobbyState state, int myId) {
        this.state = state;
        this.myId = myId;
    }

    /**
     * Turns a map path into a name for the player: {@code maps/Etoile-Filante.txt} becomes {@code Etoile-Filante}.
     */
    public static String mapLabel(String map) {
        String name = map.substring(map.lastIndexOf('/') + 1);
        int extension = name.lastIndexOf('.');
        return extension < 0 ? name : name.substring(0, extension);
    }

    /** COLT → "Colt": a new brawler type gets its label without extra code. */
    public static String brawlerLabel(BrawlerType type) {
        String name = type.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /** Returns one label per player of a team, with the leader, you and the brawler marks. */
    public List<String> playerLabels(Team team) {
        return playersOf(team).stream().map(this::label).toList();
    }

    /** Returns how many players a team has. */
    public int count(Team team) {
        return playersOf(team).size();
    }

    /** Whether I am the leader. */
    public boolean isLeader() {
        return state.leaderId() == myId;
    }

    /** Whether I can move to this team: I am in the other one and it is not full. */
    public boolean canJoin(Team team) {
        LobbyPlayerState me = me();
        return me != null && me.team() != team && count(team) < Lobby.MAX_TEAM_SIZE;
    }

    /** The brawler I chose, or null if I am not in the lobby. */
    public BrawlerType myBrawler() {
        LobbyPlayerState me = me();
        return me == null ? null : me.brawler();
    }

    /** Whether I can change the map (leader only). */
    public boolean canChooseMap() {
        return isLeader();
    }

    /** Whether I can start the match: I am the leader and both teams are ready. */
    public boolean canStart() {
        return isLeader() && state.canStart();
    }

    /** Returns the hint shown under the teams. */
    public String status() {
        if (!state.canStart()) {
            return "Each team needs at least one player";
        }
        return isLeader() ? "You can start the game" : "Waiting for the leader…";
    }

    /** Builds the label of one player, for example {@code Louis (leader, you) · Colt}. */
    private String label(LobbyPlayerState player) {
        List<String> marks = new ArrayList<>();
        if (player.id() == state.leaderId()) {
            marks.add(LEADER_MARK);
        }
        if (player.id() == myId) {
            marks.add(MY_MARK);
        }
        String name = marks.isEmpty() ? player.name() : player.name() + " (" + String.join(", ", marks) + ")";
        return name + BRAWLER_SEPARATOR + brawlerLabel(player.brawler());
    }

    /** Returns the players of a team. */
    private List<LobbyPlayerState> playersOf(Team team) {
        return state.players().stream().filter(player -> player.team() == team).toList();
    }

    /** Returns my player, or {@code null} if I am not in the lobby. */
    private LobbyPlayerState me() {
        return state.players().stream().filter(player -> player.id() == myId).findFirst().orElse(null);
    }
}
