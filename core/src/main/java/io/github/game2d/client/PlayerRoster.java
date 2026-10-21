package io.github.game2d.client;

import io.github.game2d.model.Team;
import io.github.game2d.protocol.LobbyPlayerState;
import io.github.game2d.protocol.LobbyState;
import java.util.HashMap;
import java.util.Map;

/**
 * Who is who during a match: nickname and team of each brawler id, and my own id.
 * Built from the last lobby state, because the snapshots never carry nicknames or teams.
 */
public final class PlayerRoster {
    private final Map<Integer, String> names;
    private final Map<Integer, Team> teams;
    private final int myId;

    private PlayerRoster(Map<Integer, String> names, Map<Integer, Team> teams, int myId) {
        this.names = Map.copyOf(names);
        this.teams = Map.copyOf(teams);
        this.myId = myId;
    }

    /**
     * Builds a roster.
     *
     * @param lobby the last lobby state, or {@code null}
     * @param myId  my brawler id
     */
    public static PlayerRoster from(LobbyState lobby, int myId) {
        Map<Integer, String> names = new HashMap<>();
        Map<Integer, Team> teams = new HashMap<>();
        if (lobby != null) {
            for (LobbyPlayerState player : lobby.players()) {
                names.put(player.id(), player.name());
                teams.put(player.id(), player.team());
            }
        }
        return new PlayerRoster(names, teams, myId);
    }

    /** Returns the nickname of a brawler, empty if unknown. */
    public String nameOf(int brawlerId) {
        return names.getOrDefault(brawlerId, "");
    }

    /** Returns my team, or {@code null} if unknown. */
    public Team myTeam() {
        return teams.get(myId);
    }

    /** Whether this brawler is mine. */
    public boolean isMe(int brawlerId) {
        return brawlerId == myId;
    }

    /** Whether this brawler is mine or in my team. */
    public boolean isAlly(int brawlerId) {
        Team myTeam = teams.get(myId);
        return isMe(brawlerId) ||(myTeam != null && myTeam == teams.get(brawlerId));
    }
}
