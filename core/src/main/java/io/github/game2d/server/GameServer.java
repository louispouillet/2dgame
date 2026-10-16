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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class GameServer {
    private enum Phase { LOBBY, PLAYING }

    private static final BrawlerType BRAWLER = BrawlerType.COLT;

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
    private final Map<Integer, BrawlerType> brawlerTypes = new HashMap<>();

    public GameServer(List<String> maps, String defaultMap, Function<String, Arena> mapLoader) {
        this.lobby = new Lobby(maps, defaultMap);
        this.mapLoader = mapLoader;
    }

    public synchronized int join(ClientChannel connection, String name) {
        requireLobby();
        int id = nextPlayerId;
        lobby.join(id, name);
        nextPlayerId++;
        connections.put(id, connection);
        broadcastLobby();
        return id;
    }

    public synchronized void chooseTeam(int playerId, Team team) {
        requireLobby();
        lobby.chooseTeam(playerId, team);
        broadcastLobby();
    }

    public synchronized void chooseMap(int playerId, String map) {
        requireLobby();
        lobby.chooseMap(playerId, map);
        broadcastLobby();
    }

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

    public synchronized void tick() {
        clock.step(this::simulate);
    }

    private void requireLobby() {
        if (phase == Phase.PLAYING) {
            throw new LobbyException(RefusalReason.GAME_IN_PROGRESS);
        }
    }

    private void startMatch() {
        arena = mapLoader.apply(lobby.map());
        world = new GameWorld(arena);
        inputApplier = new InputApplier(world);
        snapshotBuilder = new SnapshotBuilder(world);
        match = new KnockoutMatch();
        List<LobbyPlayer> players = lobby.players();
        for (LobbyPlayer player : players) {
            brawlerTypes.put(player.id(), BRAWLER);
        }
        placePlayers();
        phase = Phase.PLAYING;
    }

    private void placePlayers() {
        world.clear();
        Map<Team, Integer> placedPerTeam = new EnumMap<>(Team.class);
        for (LobbyPlayer player : lobby.players()) {
            int place = placedPerTeam.merge(player.team(), 1, Integer::sum) - 1;
            SpawnPoint spawn = arena.getSpawnPoints(player.team()).get(place);
            BrawlerType type = brawlerTypes.get(player.id());
            world.addPlayer(type.create(player.id(), spawn.x(), spawn.y()), player.team());
        }
    }

    private void forfeitEmptyTeams() {
        for (Team team : Team.values()) {
            if (lobby.players().stream().noneMatch(player -> player.team() == team)) {
                match.forfeit(team);
            }
        }
    }

    private void endMatch() {
        arena = null;
        world = null;
        inputApplier = null;
        snapshotBuilder = null;
        match = null;
        brawlerTypes.clear();
        phase = Phase.LOBBY;
    }

    private MatchState matchState() {
        return new MatchState(match.phase(), match.round(), match.score(Team.BLUE), match.score(Team.RED),
            match.timeLeft(), match.roundWinner(), match.matchWinner());
    }

    private void broadcastLobby() {
        List<LobbyPlayerState> players = new ArrayList<>();
        for (LobbyPlayer player : lobby.players()) {
            players.add(new LobbyPlayerState(player.id(), player.name(), player.team()));
        }
        LobbyState state = new LobbyState(players, lobby.leaderId(), lobby.map(), new ArrayList<>(lobby.maps()),
            lobby.canStart(), phase == Phase.PLAYING);
        for (ClientChannel connection : connections.values()) {
            connection.sendLobby(state);
        }
    }

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

    public void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(this::run);
        thread.setDaemon(true);
        thread.start();
    }

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
