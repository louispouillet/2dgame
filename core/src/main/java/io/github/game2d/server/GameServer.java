package io.github.game2d.server;

import io.github.game2d.model.GameWorld;
import io.github.game2d.model.Team;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.SpawnPoint;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.net.ClientChannel;
import io.github.game2d.protocol.LobbyPlayerState;
import io.github.game2d.protocol.LobbyState;
import io.github.game2d.protocol.RefusalReason;
import io.github.game2d.protocol.WorldSnapshot;
import io.github.game2d.time.TickClock;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class GameServer {
    private enum Phase { LOBBY, PLAYING }

    private static final BrawlerType[] BRAWLER_ROTATION = BrawlerType.values();

    private volatile boolean running;
    private Thread thread;
    private final Lobby lobby;
    private final Function<String, Arena> mapLoader;
    private final Map<Integer, ClientChannel> connections = new LinkedHashMap<>();
    private final TickClock clock = new TickClock();
    private Phase phase = Phase.LOBBY;
    private int nextPlayerId = 1;
    private GameWorld world;
    private InputApplier inputApplier;
    private SnapshotBuilder snapshotBuilder;

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
        Arena arena = mapLoader.apply(lobby.map());
        world = new GameWorld(arena);
        inputApplier = new InputApplier(world);
        snapshotBuilder = new SnapshotBuilder(world);
        Map<Team, Integer> placedPerTeam = new EnumMap<>(Team.class);
        List<LobbyPlayer> players = lobby.players();
        for (int index = 0; index < players.size(); index++) {
            LobbyPlayer player = players.get(index);
            int place = placedPerTeam.merge(player.team(), 1, Integer::sum) - 1;
            SpawnPoint spawn = arena.getSpawnPoints(player.team()).get(place);
            BrawlerType type = BRAWLER_ROTATION[index % BRAWLER_ROTATION.length];
            world.addPlayer(type.create(player.id(), spawn.x(), spawn.y()), player.team());
        }
        phase = Phase.PLAYING;
    }

    private void endMatch() {
        world = null;
        inputApplier = null;
        snapshotBuilder = null;
        phase = Phase.LOBBY;
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
        for (Map.Entry<Integer, ClientChannel> entry : connections.entrySet()) {
            inputApplier.apply(entry.getKey(), entry.getValue().latestInput());
        }
        world.update(clock.getStep());
        for (Map.Entry<Integer, ClientChannel> entry : connections.entrySet()) {
            WorldSnapshot snapshot = snapshotBuilder.build(tick, inputApplier::lastProcessedSequence, entry.getKey());
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
