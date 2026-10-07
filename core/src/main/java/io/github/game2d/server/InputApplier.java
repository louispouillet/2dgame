package io.github.game2d.server;

import io.github.game2d.model.GameWorld;
import io.github.game2d.model.brawler.Brawler;
import io.github.game2d.protocol.PlayerInput;

import java.util.HashMap;
import java.util.Map;

public class InputApplier {
    private final GameWorld world;
    private final Map<Integer, Integer> lastSequences = new HashMap<>();

    public InputApplier(GameWorld world) {
        this.world = world;
    }

    public void apply(int brawlerId, PlayerInput input) {
        lastSequences.put(brawlerId, input.sequence());
        Brawler brawler = world.getPlayer(brawlerId);
        if (brawler != null) {
            brawler.setDirection(input.moveX(), input.moveY());
            if (input.shooting()) {
                world.attack(brawler, input.aimX(), input.aimY());
            }
        }
    }

    public int lastProcessedSequence(int brawlerId) {
        return lastSequences.getOrDefault(brawlerId, 0);
    }

    public void forget(int brawlerId) {
        lastSequences.remove(brawlerId);
    }
}
