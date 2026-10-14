package io.github.game2d.client;

import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.brawler.Brawler;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;
import io.github.game2d.time.TickClock;
import java.util.ArrayList;
import java.util.List;

public class ClientGame {
    private static final float INTERPOLATION_DELAY = 0.1f;

    private final Arena arena;
    private final GameSession session;
    private final SnapshotBuffer buffer = new SnapshotBuffer();
    private final ClientClock clock = new ClientClock();
    private WorldSnapshot lastReceived;
    private PredictedPlayer predicted;

    public ClientGame(Arena arena, GameSession session) {
        this.arena = arena;
        this.session = session;
    }

    public WorldSnapshot update(double now, InputReader inputs) {
        clock.update(now);
        WorldSnapshot snapshot = session.channel().latestSnapshot();
        if (snapshot == null) {
            return null;
        }
        boolean isNew = snapshot != lastReceived;
        if (isNew) {
            clock.observe(snapshot.tick());
            buffer.add(snapshot, TickClock.timeOfTick(snapshot.tick()));
            lastReceived = snapshot;
        }
        BrawlerState mine = snapshot.findBrawler(session.playerId());
        if (mine != null) {
            predict(mine, isNew, inputs);
        }
        WorldSnapshot interpolated = buffer.interpolate(clock.serverTime() - INTERPOLATION_DELAY);
        return withOwnBrawler(interpolated, predictedState(mine));
    }

    private void predict(BrawlerState mine, boolean isNew, InputReader inputs) {
        if (predicted == null) {
            Brawler copy = mine.type().create(mine.id(), mine.x(), mine.y());
            predicted = new PredictedPlayer(arena, TickClock.STEP, copy);
        }
        if (isNew) {
            predicted.reconcile(mine);
        }
        clock.forEachInputTick(() -> {
            PlayerInput input = inputs.read(predicted.getX(), predicted.getY());
            session.channel().sendInput(input);
            predicted.apply(input);
        });
    }

    private BrawlerState predictedState(BrawlerState mine) {
        if (mine == null) {
            return null;
        }
        return new BrawlerState(mine.id(), predicted.getX(), predicted.getY(), mine.radius(), mine.health(), mine.maxHealth(), mine.ammo(), mine.maxAmmo(), mine.reloadProgress(), mine.type(), mine.lastProcessedInput());
    }

    private static WorldSnapshot withOwnBrawler(WorldSnapshot view, BrawlerState mine) {
        if (mine == null) {
            return view;
        }
        List<BrawlerState> brawlers = new ArrayList<>();
        for (BrawlerState brawler : view.brawlers()) {
            brawlers.add(brawler.id() == mine.id() ? mine : brawler);
        }
        return new WorldSnapshot(view.tick(), brawlers, view.projectiles(), view.match());
    }
}
