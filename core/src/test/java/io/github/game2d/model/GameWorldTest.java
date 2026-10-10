package io.github.game2d.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.TestArenas;
import io.github.game2d.model.brawler.Colt;
import io.github.game2d.model.brawler.Shelly;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GameWorldTest {
    private static final Arena ARENA = TestArenas.open(30, 20);
    private static final float X0 = 10f;
    private static final float Y0 = 10f;

    @Test
    void projectileDamagesTargetAndDisappears() {
        GameWorld world = new GameWorld(ARENA);
        Colt shooter = new Colt(world.nextId(), X0, Y0);
        Shelly target = new Shelly(world.nextId(), X0 + 2, Y0);
        world.addPlayer(shooter);
        world.addPlayer(target);

        world.attack(shooter, 1, 0);
        for (int i = 0; i < 180; i++) {
            world.update(1 / 60f);
        }

        assertTrue(target.getHealth() < target.getMaxHealth());
        assertTrue(world.getGameObjects().stream().noneMatch(o -> o instanceof Projectile));
    }

    @Test
    void burstBulletsJoinTheWorldOverTime() {
        GameWorld world = new GameWorld(ARENA);
        Colt shooter = new Colt(world.nextId(), X0, Y0);
        world.addPlayer(shooter);

        world.attack(shooter, 1, 0);
        assertEquals(1, countProjectiles(world));
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 120; i++) {
            world.update(1 / 60f);
            world.getGameObjects().stream().filter(o -> o instanceof Projectile).forEach(o -> seen.add(o.getId()));
        }

        // one attack, but more bullets appeared after the first one
        assertTrue(seen.size() > 1);
    }

    @Test
    void deadBrawlerStopsItsBurst() {
        GameWorld world = new GameWorld(ARENA);
        Colt shooter = new Colt(world.nextId(), X0, Y0);
        world.addPlayer(shooter);

        world.attack(shooter, 1, 0);
        shooter.takeDamage(shooter.getHealth());
        for (int i = 0; i < 120; i++) {
            world.update(1 / 60f);
        }

        assertTrue(countProjectiles(world) <= 1);
    }

    private static long countProjectiles(GameWorld world) {
        return world.getGameObjects().stream().filter(o -> o instanceof Projectile).count();
    }

    @Test
    void projectileDoesNotHitItsOwner() {
        GameWorld world = new GameWorld(ARENA);
        Colt shooter = new Colt(world.nextId(), X0, Y0);
        world.addPlayer(shooter);

        world.attack(shooter, 1, 0);
        world.update(1 / 60f);

        assertEquals(100, shooter.getHealth());
    }

    @Test
    void deadBrawlerIsRemovedFromWorld() {
        GameWorld world = new GameWorld(ARENA);
        Colt target = new Colt(world.nextId(), X0, Y0);
        world.addPlayer(target);

        target.takeDamage(100);
        world.update(1 / 60f);

        assertFalse(world.getGameObjects().contains(target));
    }

    @Test
    void shotMissingTheTargetDoesNoDamage() {
        GameWorld world = new GameWorld(ARENA);
        Colt shooter = new Colt(world.nextId(), X0, Y0);
        Shelly target = new Shelly(world.nextId(), X0 + 2, Y0 + 3);
        world.addPlayer(shooter);
        world.addPlayer(target);

        world.attack(shooter, 1, 0);
        for (int i = 0; i < 30; i++) {
            world.update(1 / 60f);
        }

        assertEquals(120, target.getHealth());
    }

    @Test
    void nextIdIsUniquePerWorldAndIndependentBetweenWorlds() {
        GameWorld a = new GameWorld(ARENA);
        GameWorld b = new GameWorld(ARENA);

        assertEquals(1, a.nextId());
        assertEquals(2, a.nextId());
        assertEquals(1, b.nextId());
    }

    @Test
    void shotgunPelletsGetUniqueIds() {
        GameWorld world = new GameWorld(ARENA);
        Shelly shooter = new Shelly(world.nextId(), X0, Y0);
        world.addPlayer(shooter);

        world.attack(shooter, 1, 0);

        long distinct = world.getGameObjects().stream().map(o -> o.getId()).distinct().count();
        assertEquals(world.getGameObjects().size(), distinct);
    }

    @Test
    void deadPlayerStaysRegisteredAfterRemovalFromWorld() {
        GameWorld world = new GameWorld(ARENA);
        Colt p1 = new Colt(world.nextId(), X0, Y0);
        world.addPlayer(p1);

        p1.takeDamage(100);
        world.update(1 / 60f);

        assertFalse(world.getGameObjects().contains(p1));
        assertEquals(p1, world.getPlayer(p1.getId()));
    }

    @Test
    void registeringTheSameBrawlerTwiceFails() {
        GameWorld world = new GameWorld(ARENA);
        Colt brawler = new Colt(world.nextId(), X0, Y0);
        world.addPlayer(brawler);

        assertThrows(IllegalArgumentException.class, () -> world.addPlayer(brawler));
    }

}
