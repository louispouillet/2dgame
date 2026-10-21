package io.github.game2d.client;

import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.brawler.Brawler;
import io.github.game2d.model.match.MatchPhase;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;
import io.github.game2d.time.TickClock;
import java.util.ArrayList;
import java.util.List;

/**
 * The client logic of a match, without rendering: reads the snapshots, interpolates the others,
 * predicts my own brawler and sends my inputs.
 */
public class ClientGame {
    private static final float INTERPOLATION_DELAY = 0.1f;

    private final Arena arena;
    private final GameSession session;
    private final SnapshotBuffer buffer = new SnapshotBuffer();
    private final ClientClock clock = new ClientClock();
    private WorldSnapshot lastReceived;
    private PredictedPlayer predicted;

    /**
     * Creates the logic of a match.
     *
     * @param arena   the map, used by the prediction
     * @param session the connection to the server
     */
    public ClientGame(Arena arena, GameSession session) {
        this.arena = arena;
        this.session = session;
    }

    /**
     * Runs one frame.
     *
     * @param now    local time, in seconds
     * @param inputs where my inputs come from
     * @return the world to draw (others interpolated, my brawler predicted), or {@code null} before the first snapshot
     */
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
            predict(mine, isNew, inputs, isPlaying(snapshot));
        }
        WorldSnapshot interpolated = buffer.interpolate(clock.serverTime() - INTERPOLATION_DELAY);
        return withOwnBrawler(interpolated, predictedState(mine));
    }

    /** Whether the round is being played (otherwise nobody moves). */
    private static boolean isPlaying(WorldSnapshot snapshot) {
        return snapshot.match() == null || snapshot.match().phase() == MatchPhase.PLAYING;
    }

    /**
     * Corrects the prediction with the server state, then sends and simulates the inputs of this frame.
     * Outside of the play phase the input is neutral.
     */
    private void predict(BrawlerState mine, boolean isNew, InputReader inputs, boolean playing) {
        if (predicted == null) {
            Brawler copy = mine.type().create(mine.id(), mine.x(), mine.y());
            predicted = new PredictedPlayer(arena, TickClock.STEP, copy);
        }
        if (isNew) {
            predicted.reconcile(mine);
        }
        clock.forEachInputTick(() -> {
            PlayerInput input = playing ? inputs.read(predicted.getX(), predicted.getY()) : inputs.idle();
            session.channel().sendInput(input);
            predicted.apply(input);
        });
    }

    /**
     * Builds my brawler state with the predicted position, or {@code null} if I am not in the snapshot.
     */
    private BrawlerState predictedState(BrawlerState mine) {
        if (mine == null) {
            return null;
        }
        float facing = mine.attacking() ? mine.facing() : predicted.getFacing();
        return new BrawlerState(mine.id(), mine.type(), predicted.getX(), predicted.getY(), mine.radius(), facing,
            mine.health(), mine.maxHealth(), mine.ammo(), mine.maxAmmo(), mine.reloadProgress(),
            predicted.isMoving(), mine.attacking(), mine.shotsFired(), mine.lastProcessedInput());
    }

    /** Replaces my brawler in a snapshot with its predicted state. */
    private static WorldSnapshot withOwnBrawler(WorldSnapshot view, BrawlerState mine) {
        if (mine == null) {
            return view;
        }
        List<BrawlerState> brawlers = new ArrayList<>();
        for (BrawlerState brawler : view.brawlers()) {
            brawlers.add(brawler.id() == mine.id() ? mine : brawler);
        }
        return new WorldSnapshot(view.tick(), brawlers, view.projectiles(), view.match(), view.followedId());
    }
}
