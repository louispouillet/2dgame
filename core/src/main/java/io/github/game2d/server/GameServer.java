package io.github.game2d.server;

import io.github.game2d.model.GameWorld;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.SpawnPoint;
import io.github.game2d.model.brawler.Brawler;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.net.ClientChannel;
import io.github.game2d.protocol.WorldSnapshot;
import io.github.game2d.time.TickClock;
import java.util.HashMap;
import java.util.Map;

public class GameServer {
    private static final BrawlerType[] BRAWLER_ROTATION = BrawlerType.values();
    private static final int[] SPAWN_ORDER = {1, 4, 2, 5, 3, 6};

    private volatile boolean running;
    private Thread thread;
    private final GameWorld world;
    private final Arena arena;
    private final InputApplier inputApplier;
    private final SnapshotBuilder snapshotBuilder;
    private final Map<Integer, ClientChannel> connections = new HashMap<>();
    private final TickClock clock = new TickClock();

    public GameServer(Arena arena) {
        this.arena = arena;
        this.world = new GameWorld(arena);
        this.inputApplier = new InputApplier(world);
        this.snapshotBuilder = new SnapshotBuilder(world);
    }

    public synchronized int join(ClientChannel connection) {
        if (connections.size() >= SPAWN_ORDER.length) {
            throw new IllegalStateException("The server is full");
        }
        SpawnPoint spawn = arena.getSpawnPoint(SPAWN_ORDER[connections.size()]);
        BrawlerType type = BRAWLER_ROTATION[connections.size() % BRAWLER_ROTATION.length];
        Brawler brawler = type.create(world.nextId(), spawn.x(), spawn.y());
        world.addPlayer(brawler);
        connections.put(brawler.getId(), connection);
        return brawler.getId();
    }

    public synchronized void leave(int brawlerId) {
        if (connections.remove(brawlerId) != null) {
            world.removePlayer(brawlerId);
            inputApplier.forget(brawlerId);
        }
    }

    public synchronized void tick() {
        clock.step(this::simulate);
    }

    private synchronized void simulate(int tick) {
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
