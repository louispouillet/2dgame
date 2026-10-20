package io.github.game2d.server;

import io.github.game2d.model.GameWorld;
import io.github.game2d.model.brawler.Brawler;
import io.github.game2d.protocol.PlayerInput;

import java.util.HashMap;
import java.util.Map;

/**
 * Turns the inputs of the players into actions on the world, and remembers the last input number of each player
 * (the client uses it to replay its prediction).
 */
public class InputApplier {
    private final GameWorld world;
    private final Map<Integer, Integer> lastSequences = new HashMap<>();

    /** Creates an applier for a world. */
    public InputApplier(GameWorld world) {
        this.world = world;
    }

    /** Confirms an input without applying it (countdown, between rounds). */
    public void acknowledge(int brawlerId, PlayerInput input) {
        lastSequences.put(brawlerId, input.sequence());
    }

    /**
     * Applies an input to a brawler: movement, then attack if shooting.
     * Values that are not finite or a null aim are ignored, so a bad client cannot break the world.
     */
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

    /** Returns the number of the last input confirmed for a player, 0 if none. */
    public int lastProcessedSequence(int brawlerId) {
        return lastSequences.getOrDefault(brawlerId, 0);
    }

    /** Forgets a player that left. */
    public void forget(int brawlerId) {
        lastSequences.remove(brawlerId);
    }
}
