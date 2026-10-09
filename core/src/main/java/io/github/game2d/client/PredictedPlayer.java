package io.github.game2d.client;

import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.brawler.Brawler;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.PlayerInput;

import java.util.ArrayList;
import java.util.List;

public class PredictedPlayer {
    private final Arena arena;
    private final float dt;
    private final Brawler brawler;
    private int lastAcknowledged;
    private final List<PlayerInput> pending = new ArrayList<>();

    public PredictedPlayer(Arena arena, float dt, Brawler brawler) {
        this.arena = arena;
        this.dt = dt;
        this.brawler = brawler;
    }

    public void apply(PlayerInput input) {
        pending.add(input);
        simulate(input);
    }

    public void reconcile(BrawlerState brawlerState){
        if (brawlerState.lastProcessedInput() < lastAcknowledged) {
            return;
        }
        lastAcknowledged = brawlerState.lastProcessedInput();
        brawler.correctPositionTo(brawlerState.x(), brawlerState.y(), arena);
        pending.removeIf(input -> input.sequence() <= brawlerState.lastProcessedInput());
        for (PlayerInput input : pending) {
            simulate(input);
        }
    }

    private void simulate(PlayerInput input) {
        brawler.setDirection(input.moveX(), input.moveY());
        brawler.update(dt, arena);
    }

    public float getX() {
        return brawler.getX();
    }

    public float getY() {
        return brawler.getY();
    }
}
