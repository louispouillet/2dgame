package io.github.game2d.server;

import io.github.game2d.model.GameWorld;
import io.github.game2d.model.Team;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.SpawnPoint;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.model.match.KnockoutMatch;
import io.github.game2d.model.match.MatchEvent;
import io.github.game2d.model.match.MatchPhase;
import io.github.game2d.net.ClientChannel;
import io.github.game2d.protocol.LobbyPlayerState;
import io.github.game2d.protocol.LobbyState;
import io.github.game2d.protocol.MatchState;
import io.github.game2d.protocol.RefusalReason;
import io.github.game2d.protocol.WorldSnapshot;
import io.github.game2d.time.TickClock;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The authority of the game. It has two phases: {@code LOBBY} (players choose, no world) and {@code PLAYING}
 * (the world exists and a Knockout match runs). Every public method is synchronized: the network threads call
 * them while the game thread ticks.
 */
public class GameServer {
    private enum Phase { LOBBY, PLAYING }

    private volatile boolean running;
    private Thread thread;
    private final Lobby lobby;
    private final Function<String, Arena> mapLoader;
    private final Map<Integer, ClientChannel> connections = new LinkedHashMap<>();
    private final TickClock clock = new TickClock();
    private Phase phase = Phase.LOBBY;
    private int nextPlayerId = 1;
    private Arena arena;
    private GameWorld world;
    private InputApplier inputApplier;
    private SnapshotBuilder snapshotBuilder;
    private KnockoutMatch match;

    /**
     * Creates a server in the lobby phase.
     *
     * @param maps       paths of the available maps
     * @param defaultMap map chosen at the start, one of {@code maps}
     * @param mapLoader  turns a map path into an arena (injected, so that tests can use small maps)
     */
    public GameServer(List<String> maps, String defaultMap, Function<String, Arena> mapLoader) {
        this.lobby = new Lobby(maps, defaultMap);
        this.mapLoader = mapLoader;
    }

    /**
     * Adds a player to the lobby and sends the new lobby state to everybody.
     *
     * @param connection how to reach this player
     * @param name       nickname asked
     * @return the id of the player, also the id of its brawler
     * @throws LobbyException if the player is refused
     */
    public synchronized int join(ClientChannel connection, String name) {
        requireLobby();
        int id = nextPlayerId;
        lobby.join(id, name);
        nextPlayerId++;
        connections.put(id, connection);
        broadcastLobby();
        return id;
    }

    /**
     * Moves a player to a team.
     *
     * @throws LobbyException if the team is full or a match is running
     */
    public synchronized void chooseTeam(int playerId, Team team) {
        requireLobby();
        lobby.chooseTeam(playerId, team);
        broadcastLobby();
    }

    /**
     * Changes the brawler of a player.
     *
     * @throws LobbyException if the brawler is unknown or a match is running
     */
    public synchronized void chooseBrawler(int playerId, BrawlerType brawler) {
        requireLobby();
        lobby.chooseBrawler(playerId, brawler);
        broadcastLobby();
    }

    /**
     * Changes the map.
     *
     * @throws LobbyException if the player is not the leader, the map is unknown or a match is running
     */
    public synchronized void chooseMap(int playerId, String map) {
        requireLobby();
        lobby.chooseMap(playerId, map);
        broadcastLobby();
    }

    /**
     * Starts the match.
     *
     * @throws LobbyException if the player is not the leader, the teams are not ready or a match is running
     */
    public synchronized void requestStart(int playerId) {
        requireLobby();
        if (!lobby.isLeader(playerId)) {
            throw new LobbyException(RefusalReason.NOT_LEADER);
        }
        if (!lobby.canStart()) {
            throw new LobbyException(RefusalReason.TEAMS_NOT_READY);
        }
        startMatch();
        broadcastLobby();
    }

    /**
     * Removes a player. In a match, its brawler disappears and a team left without player forfeits;
     * when nobody remains the server goes back to the lobby.
     */
    public synchronized void leave(int playerId) {
        if (connections.remove(playerId) == null) {
            return;
        }
        lobby.leave(playerId);
        if (phase == Phase.PLAYING) {
            world.removePlayer(playerId);
            inputApplier.forget(playerId);
            if (connections.isEmpty()) {
                endMatch();
            } else {
                forfeitEmptyTeams();
            }
        }
        broadcastLobby();
    }

    /** Runs exactly one tick, without waiting (used by the tests). */
    public synchronized void tick() {
        clock.step(this::simulate);
    }

    /** Refuses lobby changes while a match is running. */
    private void requireLobby() {
        if (phase == Phase.PLAYING) {
            throw new LobbyException(RefusalReason.GAME_IN_PROGRESS);
        }
    }

    /** Loads the chosen map and creates the world and the match. */
    private void startMatch() {
        arena = mapLoader.apply(lobby.map());
        world = new GameWorld(arena);
        inputApplier = new InputApplier(world);
        snapshotBuilder = new SnapshotBuilder(world);
        match = new KnockoutMatch();
        placePlayers();
        phase = Phase.PLAYING;
    }

    /**
     * Empties the world and puts every player back on the spawn point of its team, with the brawler chosen in the lobby.
     */
    private void placePlayers() {
        world.clear();
        Map<Team, Integer> placedPerTeam = new EnumMap<>(Team.class);
        for (LobbyPlayer player : lobby.players()) {
            int place = placedPerTeam.merge(player.team(), 1, Integer::sum) - 1;
            SpawnPoint spawn = arena.getSpawnPoints(player.team()).get(place);
            // The lobby cannot change during a match: everyone keeps the brawler chosen before the start
            world.addPlayer(player.brawler().create(player.id(), spawn.x(), spawn.y()), player.team());
        }
    }

    /** Makes lose a team that has no player left. */
    private void forfeitEmptyTeams() {
        for (Team team : Team.values()) {
            if (lobby.players().stream().noneMatch(player -> player.team() == team)) {
                match.forfeit(team);
            }
        }
    }

    /** Throws the world away and goes back to the lobby phase. */
    private void endMatch() {
        arena = null;
        world = null;
        inputApplier = null;
        snapshotBuilder = null;
        match = null;
        phase = Phase.LOBBY;
    }

    /** Copies the state of the match into a protocol message. */
    private MatchState matchState() {
        return new MatchState(match.phase(), match.round(), match.score(Team.BLUE), match.score(Team.RED),
            match.timeLeft(), match.roundWinner(), match.matchWinner());
    }

    /** Sends the lobby state to every connected player. */
    private void broadcastLobby() {
        List<LobbyPlayerState> players = new ArrayList<>();
        for (LobbyPlayer player : lobby.players()) {
            players.add(new LobbyPlayerState(player.id(), player.name(), player.team(), player.brawler()));
        }
        LobbyState state = new LobbyState(players, lobby.leaderId(), lobby.map(), new ArrayList<>(lobby.maps()),
            lobby.canStart(), phase == Phase.PLAYING);
        for (ClientChannel connection : connections.values()) {
            connection.sendLobby(state);
        }
    }

    /**
     * One tick: applies the inputs, advances the world while the round is played, updates the match,
     * then sends one snapshot per player.
     */
    private synchronized void simulate(int tick) {
        if (phase != Phase.PLAYING) {
            return;
        }
        boolean playing = match.phase() == MatchPhase.PLAYING;
        for (Map.Entry<Integer, ClientChannel> entry : connections.entrySet()) {
            if (playing) {
                inputApplier.apply(entry.getKey(), entry.getValue().latestInput());
            } else {
                inputApplier.acknowledge(entry.getKey(), entry.getValue().latestInput());
            }
        }
        if (playing) {
            world.update(clock.getStep());
        }
        MatchEvent event = match.update(clock.getStep(), world::aliveCount);
        if (event == MatchEvent.ROUND_STARTED) {
            placePlayers();
        } else if (event == MatchEvent.MATCH_FINISHED) {
            endMatch();
            broadcastLobby();
            return;
        }
        MatchState state = matchState();
        for (Map.Entry<Integer, ClientChannel> entry : connections.entrySet()) {
            WorldSnapshot snapshot = snapshotBuilder.build(tick, inputApplier::lastProcessedSequence, entry.getKey(), state);
            entry.getValue().sendSnapshot(snapshot);
        }
    }

    /** Starts the game thread, which ticks at 30 Hz. */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(this::run);
        thread.setDaemon(true);
        thread.start();
    }

    /** Stops the game thread and waits for it. */
    public void stop() {
        if (thread == null) {
            return;
        }
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Loop of the game thread. */
    private void run() {
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float frameTime = (now - last) / 1_000_000_000f;
            last = now;
            clock.advance(frameTime, this::simulate);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
