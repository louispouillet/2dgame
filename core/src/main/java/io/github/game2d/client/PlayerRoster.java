package io.github.game2d.client;

import io.github.game2d.model.Team;
import io.github.game2d.protocol.LobbyPlayerState;
import io.github.game2d.protocol.LobbyState;
import java.util.HashMap;
import java.util.Map;

public final class PlayerRoster {
    private final Map<Integer, String> names;
    private final Map<Integer, Team> teams;
    private final int myId;

    private PlayerRoster(Map<Integer, String> names, Map<Integer, Team> teams, int myId) {
        this.names = Map.copyOf(names);
        this.teams = Map.copyOf(teams);
        this.myId = myId;
    }

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

    public String nameOf(int brawlerId) {
        return names.getOrDefault(brawlerId, "");
    }

    public Team myTeam() {
        return teams.get(myId);
    }

    public boolean isMe(int brawlerId) {
        return brawlerId == myId;
    }

    public boolean isAlly(int brawlerId) {
        Team myTeam = teams.get(myId);
        return isMe(brawlerId) ||(myTeam != null && myTeam == teams.get(brawlerId));
    }
}
