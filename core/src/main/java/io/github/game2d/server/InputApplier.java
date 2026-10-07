package io.github.game2d.server;

import io.github.game2d.model.GameWorld;
import io.github.game2d.model.brawler.Brawler;
import io.github.game2d.protocol.PlayerInput;

public class InputApplier {
    private final GameWorld world;

    public InputApplier(GameWorld world) {
        this.world = world;
    }

    public void apply(int brawlerId, PlayerInput input) {
        Brawler brawler = world.getPlayer(brawlerId);
        if (brawler != null) {
            brawler.setDirection(input.moveX(), input.moveY());
            if (input.shooting()) {
                world.attack(brawler, input.aimX(), input.aimY());
            }
        }
    }
}
