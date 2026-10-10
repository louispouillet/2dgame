package io.github.game2d.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.ArenaLoader;
import io.github.game2d.model.brawler.Colt;
import io.github.game2d.model.brawler.Shelly;
import org.junit.jupiter.api.Test;

class GameWorldCollisionTest {
    private static final Arena ARENA = ArenaLoader.parse("""
        #########
        #1..#..2#
        #..W....#
        #..~....#
        #########
        """);
    private static final float TICK = 1 / 30f;

    private static void run(GameWorld world, float seconds) {
        for (float time = 0; time < seconds; time += TICK) {
            world.update(TICK);
        }
    }

    private static boolean hasProjectiles(GameWorld world) {
        return world.getGameObjects().stream().anyMatch(object -> object instanceof Projectile);
    }

    @Test
    void wallStopsTheShotAndProtectsTheTargetBehindIt() {
        GameWorld world = new GameWorld(ARENA);
        Colt shooter = new Colt(world.nextId(), 2.5f, 3.5f);
        Shelly target = new Shelly(world.nextId(), 6.5f, 3.5f);
        world.addPlayer(shooter);
        world.addPlayer(target);

        world.attack(shooter, 1, 0);
        // assez longtemps pour que toute la rafale soit partie et ait atteint le mur
        run(world, 3f);

        assertFalse(hasProjectiles(world));
        assertEquals(120, target.getHealth());
    }

    @Test
    void shotFliesOverWater() {
        GameWorld world = new GameWorld(ARENA);
        Colt shooter = new Colt(world.nextId(), 1.5f, 2.5f);
        Shelly target = new Shelly(world.nextId(), 5.5f, 2.5f);
        world.addPlayer(shooter);
        world.addPlayer(target);

        world.attack(shooter, 1, 0);
        run(world, 1f);

        assertTrue(target.getHealth() < 120);
    }

    @Test
    void shotGoesThroughABush() {
        GameWorld world = new GameWorld(ARENA);
        Colt shooter = new Colt(world.nextId(), 1.5f, 1.5f);
        Shelly target = new Shelly(world.nextId(), 5.5f, 1.5f);
        world.addPlayer(shooter);
        world.addPlayer(target);

        world.attack(shooter, 1, 0);
        run(world, 1f);

        assertTrue(target.getHealth() < 120);
    }
}
