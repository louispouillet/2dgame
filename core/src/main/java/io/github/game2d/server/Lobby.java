package io.github.game2d.server;

import io.github.game2d.model.Team;
import io.github.game2d.protocol.RefusalReason;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class Lobby {
    public static final int MAX_TEAM_SIZE = 3;
    public static final int MAX_NAME_LENGTH = 16;
    public static final int NO_LEADER = -1;

    private static final int MAX_PLAYERS = 2 * MAX_TEAM_SIZE;
    private static final Pattern VALID_NAME = Pattern.compile("[\\p{L}\\p{N}_-]{1," + MAX_NAME_LENGTH + "}");

    private final List<LobbyPlayer> players = new ArrayList<>();
    private final List<String> maps;
    private String map;

    public Lobby(List<String> maps, String defaultMap) {
        this.maps = List.copyOf(maps);
        if (!this.maps.contains(defaultMap)) {
            throw new IllegalArgumentException("Unknown default map " + defaultMap);
        }
        this.map = defaultMap;
    }

    public LobbyPlayer join(int id, String name) {
        if (players.size() >= MAX_PLAYERS) {
            throw new LobbyException(RefusalReason.LOBBY_FULL);
        }
        String cleanName = name == null ? "" : name.strip();
        if (!VALID_NAME.matcher(cleanName).matches()) {
            throw new LobbyException(RefusalReason.INVALID_NAME);
        }
        if (players.stream().anyMatch(player -> player.name().equalsIgnoreCase(cleanName))) {
            throw new LobbyException(RefusalReason.NAME_TAKEN);
        }
        LobbyPlayer player = new LobbyPlayer(id, cleanName, smallestTeam());
        players.add(player);
        return player;
    }

    public void leave(int id) {
        players.removeIf(player -> player.id() == id);
    }

    public void chooseTeam(int id, Team team) {
        int index = indexOf(id);
        LobbyPlayer player = players.get(index);
        if (player.team() == team) {
            return;
        }
        if (teamSize(team) >= MAX_TEAM_SIZE) {
            throw new LobbyException(RefusalReason.TEAM_FULL);
        }
        players.set(index, new LobbyPlayer(id, player.name(), team));
    }

    public void chooseMap(int requesterId, String map) {
        if (!isLeader(requesterId)) {
            throw new LobbyException(RefusalReason.NOT_LEADER);
        }
        if (!maps.contains(map)) {
            throw new LobbyException(RefusalReason.UNKNOWN_MAP);
        }
        this.map = map;
    }

    public boolean canStart() {
        return isPlayable(teamSize(Team.BLUE)) && isPlayable(teamSize(Team.RED));
    }

    public List<LobbyPlayer> players() {
        return List.copyOf(players);
    }

    public int leaderId() {
        return players.isEmpty() ? NO_LEADER : players.getFirst().id();
    }

    public boolean isLeader(int id) {
        return !players.isEmpty() && leaderId() == id;
    }

    public String map() {
        return map;
    }

    public List<String> maps() {
        return maps;
    }

    private static boolean isPlayable(int teamSize) {
        return teamSize >= 1 && teamSize <= MAX_TEAM_SIZE;
    }

    private Team smallestTeam() {
        return teamSize(Team.BLUE) <= teamSize(Team.RED) ? Team.BLUE : Team.RED;
    }

    private int teamSize(Team team) {
        return (int) players.stream().filter(player -> player.team() == team).count();
    }

    private int indexOf(int id) {
        for (int index = 0; index < players.size(); index++) {
            if (players.get(index).id() == id) {
                return index;
            }
        }
        throw new IllegalArgumentException("Unknown player " + id);
    }
}
