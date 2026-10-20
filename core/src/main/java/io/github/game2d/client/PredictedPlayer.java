package io.github.game2d.client;

import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.brawler.Brawler;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.PlayerInput;

import java.util.ArrayList;
import java.util.List;

/**
 * Client side prediction of my own brawler: my inputs are applied at once on a copy of the brawler,
 * and corrected when the server confirms them (inputs not confirmed yet are replayed on top of the server state).
 */
public class PredictedPlayer {
    private final Arena arena;
    private final float dt;
    private final Brawler brawler;
    private int lastAcknowledged;
    private final List<PlayerInput> pending = new ArrayList<>();

    /**
     * Creates a predicted player.
     *
     * @param arena   the map
     * @param dt      duration of one input step, in seconds
     * @param brawler a copy of my brawler, which this class moves
     */
    public PredictedPlayer(Arena arena, float dt, Brawler brawler) {
        this.arena = arena;
        this.dt = dt;
        this.brawler = brawler;
    }

    /** Applies an input now and keeps it until the server confirms it. */
    public void apply(PlayerInput input) {
        pending.add(input);
        simulate(input);
    }

    /**
     * Takes the position given by the server, then replays the inputs it has not confirmed yet.
     * An older state than one already used is ignored.
     */
    public void reconcile(BrawlerState brawlerState){
        if (brawlerState.lastProcessedInput() < lastAcknowledged) {
            return;
        }
        lastAcknowledged = brawlerState.lastProcessedInput();
        brawler.correctPositionTo(brawlerState.x(), brawlerState.y(), arena);
        brawler.correctFacingTo(brawlerState.facing());
        pending.removeIf(input -> input.sequence() <= brawlerState.lastProcessedInput());
        for (PlayerInput input : pending) {
            simulate(input);
        }
    }

    /** Applies one input to the copy of the brawler. */
    private void simulate(PlayerInput input) {
        brawler.setDirection(input.moveX(), input.moveY());
        brawler.update(dt, arena);
    }

    /** Returns the predicted horizontal position. */
    public float getX() {
        return brawler.getX();
    }

    /** Returns the predicted vertical position. */
    public float getY() {
        return brawler.getY();
    }

    /** Returns the predicted direction, in radians. */
    public float getFacing() {
        return brawler.getFacing();
    }

    /** Whether the predicted brawler is walking. */
    public boolean isMoving() {
        return brawler.isMoving();
    }
}
