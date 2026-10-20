package io.github.game2d.server;

import io.github.game2d.model.Team;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.protocol.RefusalReason;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The waiting room before a match: players, teams, brawlers and map.
 * Pure Java without network. Not thread-safe: {@link GameServer} calls it under its lock.
 */
public final class Lobby {
    /** Players per team, at most. */
    public static final int MAX_TEAM_SIZE = 3;
    /** Longest nickname. */
    public static final int MAX_NAME_LENGTH = 16;
    /** Leader id when the lobby is empty. */
    public static final int NO_LEADER = -1;
    /** Brawler given to a player who enters. */
    public static final BrawlerType DEFAULT_BRAWLER = BrawlerType.COLT;

    private static final int MAX_PLAYERS = 2 * MAX_TEAM_SIZE;
    private static final Pattern VALID_NAME = Pattern.compile("[\\p{L}\\p{N}_-]{1," + MAX_NAME_LENGTH + "}");

    private final List<LobbyPlayer> players = new ArrayList<>();
    private final List<String> maps;
    private String map;

    /**
     * Creates an empty lobby.
     *
     * @param maps       the maps the leader can choose from
     * @param defaultMap map chosen at the start, one of {@code maps}
     * @throws IllegalArgumentException if the default map is not in the list
     */
    public Lobby(List<String> maps, String defaultMap) {
        this.maps = List.copyOf(maps);
        if (!this.maps.contains(defaultMap)) {
            throw new IllegalArgumentException("Unknown default map " + defaultMap);
        }
        this.map = defaultMap;
    }

    /**
     * Adds a player in the team with the fewest players (blue when equal).
     *
     * @param name nickname; surrounding spaces are removed, 1 to 16 letters, digits, {@code _} or {@code -}, unique ignoring case
     * @throws LobbyException if the lobby is full or the nickname is invalid or taken
     */
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
        LobbyPlayer player = new LobbyPlayer(id, cleanName, smallestTeam(), DEFAULT_BRAWLER);
        players.add(player);
        return player;
    }

    /** Removes a player. If it was the leader, the next player becomes leader. */
    public void leave(int id) {
        players.removeIf(player -> player.id() == id);
    }

    /**
     * Moves a player to a team (nothing if it is already there).
     *
     * @throws LobbyException if the team is full
     */
    public void chooseTeam(int id, Team team) {
        int index = indexOf(id);
        LobbyPlayer player = players.get(index);
        if (player.team() == team) {
            return;
        }
        if (teamSize(team) >= MAX_TEAM_SIZE) {
            throw new LobbyException(RefusalReason.TEAM_FULL);
        }
        players.set(index, player.withTeam(team));
    }

    /** Several players, even in the same team, may play the same brawler. */
    public void chooseBrawler(int id, BrawlerType brawler) {
        if (brawler == null) {
            throw new LobbyException(RefusalReason.UNKNOWN_BRAWLER);
        }
        int index = indexOf(id);
        players.set(index, players.get(index).withBrawler(brawler));
    }

    /**
     * Changes the map.
     *
     * @throws LobbyException if the requester is not the leader or the map is unknown
     */
    public void chooseMap(int requesterId, String map) {
        if (!isLeader(requesterId)) {
            throw new LobbyException(RefusalReason.NOT_LEADER);
        }
        if (!maps.contains(map)) {
            throw new LobbyException(RefusalReason.UNKNOWN_MAP);
        }
        this.map = map;
    }

    /** Whether each team has between 1 and 3 players. */
    public boolean canStart() {
        return isPlayable(teamSize(Team.BLUE)) && isPlayable(teamSize(Team.RED));
    }

    /** Returns a copy of the players, in order of arrival. */
    public List<LobbyPlayer> players() {
        return List.copyOf(players);
    }

    /** Returns the id of the leader (the first player), or {@link #NO_LEADER}. */
    public int leaderId() {
        return players.isEmpty() ? NO_LEADER : players.getFirst().id();
    }

    /** Whether this player is the leader. */
    public boolean isLeader(int id) {
        return !players.isEmpty() && leaderId() == id;
    }

    /** Returns the chosen map. */
    public String map() {
        return map;
    }

    /** Returns the maps that can be chosen. */
    public List<String> maps() {
        return maps;
    }

    /** Whether a team of this size can play. */
    private static boolean isPlayable(int teamSize) {
        return teamSize >= 1 && teamSize <= MAX_TEAM_SIZE;
    }

    /** Returns the team with the fewest players, blue when equal. */
    private Team smallestTeam() {
        return teamSize(Team.BLUE) <= teamSize(Team.RED) ? Team.BLUE : Team.RED;
    }

    /** Returns the number of players in a team. */
    private int teamSize(Team team) {
        return (int) players.stream().filter(player -> player.team() == team).count();
    }

    /**
     * Returns the position of a player in the list.
     *
     * @throws IllegalArgumentException if the player is unknown
     */
    private int indexOf(int id) {
        for (int index = 0; index < players.size(); index++) {
            if (players.get(index).id() == id) {
                return index;
            }
        }
        throw new IllegalArgumentException("Unknown player " + id);
    }
}
