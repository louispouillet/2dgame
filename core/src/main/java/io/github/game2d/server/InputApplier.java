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

    public void acknowledge(int brawlerId, PlayerInput input) {
        lastSequences.put(brawlerId, input.sequence());
    }

    public void apply(int brawlerId, PlayerInput input) {
        lastSequences.put(brawlerId, input.sequence());
        Brawler brawler = world.getPlayer(brawlerId);
        if (brawler != null) {
            if (Float.isFinite(input.moveX()) && Float.isFinite(input.moveY())) {
                brawler.setDirection(input.moveX(), input.moveY());
            } else {
                brawler.setDirection(0, 0);
            }
            if (input.shooting()) {
                if (!Float.isFinite(input.aimX()) || !Float.isFinite(input.aimY())) {
                    return;
                }
                float length = (float) Math.sqrt(input.aimX() * input.aimX() + input.aimY() * input.aimY());
                if (length == 0 || !Float.isFinite(length)) {
                    return;
                }
                world.attack(brawler, input.aimX() / length, input.aimY() / length);
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
