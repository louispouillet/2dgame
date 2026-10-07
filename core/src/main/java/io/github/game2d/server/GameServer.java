package io.github.game2d.server;

import io.github.game2d.model.GameWorld;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.SpawnPoint;
import io.github.game2d.model.brawler.Brawler;
import io.github.game2d.model.brawler.Colt;
import io.github.game2d.model.brawler.Shelly;
import io.github.game2d.net.ClientChannel;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.HashMap;
import java.util.Map;

public class GameServer {
    private static final int TICK_RATE = 30;

    private volatile boolean running;
    private Thread thread;
    private final GameWorld world;
    private final Arena arena;
    private final InputApplier inputApplier;
    private final SnapshotBuilder snapshotBuilder;
    private final Map<Integer, ClientChannel> connections = new HashMap<>();
    private final FixedTimestep timestep = new FixedTimestep(1f / TICK_RATE);

    public GameServer(Arena arena) {
        this.arena = arena;
        this.world = new GameWorld(arena);
        this.inputApplier = new InputApplier(world);
        this.snapshotBuilder = new SnapshotBuilder(world);
    }

    public synchronized int join(ClientChannel connection) {
        SpawnPoint spawn = arena.getSpawnPoint(connections.size() + 1);
        Brawler brawler;
        if (connections.isEmpty()) {
            brawler = new Shelly(world.nextId(), spawn.x(), spawn.y());
        } else {
            brawler = new Colt(world.nextId(), spawn.x(), spawn.y());
        }
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
        for (Map.Entry<Integer, ClientChannel> entry : connections.entrySet()) {
            inputApplier.apply(entry.getKey(), entry.getValue().latestInput());
        }
        world.update(timestep.getStep());
        WorldSnapshot snapshot = snapshotBuilder.build(timestep.getTicks(), inputApplier::lastProcessedSequence);
        for (ClientChannel connection : connections.values()) {
            connection.sendSnapshot(snapshot);
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
            timestep.advance(frameTime, this::tick);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
