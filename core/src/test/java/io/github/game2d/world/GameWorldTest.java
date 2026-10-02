package io.github.game2d.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.brawler.Colt;
import io.github.game2d.brawler.Shelly;
import io.github.game2d.model.Projectile;
import org.junit.jupiter.api.Test;

class GameWorldTest {
    @Test
    void projectileDamagesTargetAndDisappears() {
        GameWorld world = new GameWorld();
        Colt shooter = new Colt(0, 0);
        Shelly target = new Shelly(2, 0);
        world.add(shooter);
        world.add(target);

        world.attack(shooter, 1, 0);
        for (int i = 0; i < 30; i++) {
            world.update(1 / 60f);
        }

        assertEquals(120 - 30, target.getHealth());
        assertTrue(world.getGameObjects().stream().noneMatch(o -> o instanceof Projectile));
    }

    @Test
    void projectileDoesNotHitItsOwner() {
        GameWorld world = new GameWorld();
        Colt shooter = new Colt(0, 0);
        world.add(shooter);

        world.attack(shooter, 1, 0);
        world.update(1 / 60f);

        assertEquals(100, shooter.getHealth());
    }

    @Test
    void deadBrawlerIsRemovedFromWorld() {
        GameWorld world = new GameWorld();
        Colt target = new Colt(0, 0);
        world.add(target);

        target.takeDamage(100);
        world.update(1 / 60f);

        assertFalse(world.getGameObjects().contains(target));
    }

    @Test
    void shotMissingTheTargetDoesNoDamage() {
        GameWorld world = new GameWorld();
        Colt shooter = new Colt(0, 0);
        Shelly target = new Shelly(2, 3);
        world.add(shooter);
        world.add(target);

        world.attack(shooter, 1, 0);
        for (int i = 0; i < 30; i++) {
            world.update(1 / 60f);
        }

        assertEquals(120, target.getHealth());
    }
}
