package io.github.game2d.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.TestArenas;
import io.github.game2d.model.brawler.Colt;
import io.github.game2d.model.brawler.Shelly;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;
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
        for (int i = 0; i < 30; i++) {
            world.update(1 / 60f);
        }

        assertEquals(120 - 30, target.getHealth());
        assertTrue(world.getGameObjects().stream().noneMatch(o -> o instanceof Projectile));
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
    void inputIsRoutedToThePlayersBrawler() {
        GameWorld world = new GameWorld(ARENA);
        Colt p1 = new Colt(world.nextId(), X0, Y0);
        Colt p2 = new Colt(world.nextId(), X0 + 5, Y0);
        world.addPlayer(p1);
        world.addPlayer(p2);

        world.applyInput(p2.getId(), new PlayerInput(1, 0, 1, 0, false));
        world.update(1f);

        assertEquals(X0, p1.getX());
        assertTrue(p2.getX() > X0 + 5);
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

    @Test
    void snapshotContainsEveryLivingBrawler() {
        GameWorld world = new GameWorld(ARENA);
        Colt colt = new Colt(world.nextId(), X0 + 1, Y0 + 2);
        Shelly shelly = new Shelly(world.nextId(), X0 + 3, Y0 + 4);
        world.addPlayer(colt);
        world.addPlayer(shelly);

        WorldSnapshot snapshot = world.snapshot();

        assertEquals(2, snapshot.brawlers().size());
        BrawlerState state = snapshot.brawlers().stream().filter(b -> b.id() == shelly.getId()).findFirst().orElseThrow();
        assertEquals(X0 + 3, state.x());
        assertEquals(Y0 + 4, state.y());
        assertEquals(0.25f, state.radius());
        assertEquals(120, state.health());
        assertEquals(120, state.maxHealth());
        assertEquals(3, state.ammo());
        assertEquals(3, state.maxAmmo());
    }

    @Test
    void snapshotContainsProjectilesWithTheirRadius() {
        GameWorld world = new GameWorld(ARENA);
        Shelly shooter = new Shelly(world.nextId(), X0, Y0);
        world.addPlayer(shooter);

        world.attack(shooter, 1, 0);
        WorldSnapshot snapshot = world.snapshot();

        assertEquals(5, snapshot.projectiles().size());
        assertEquals(0.05f, snapshot.projectiles().get(0).radius());
    }

    @Test
    void snapshotOmitsDeadBrawlers() {
        GameWorld world = new GameWorld(ARENA);
        Colt alive = new Colt(world.nextId(), X0, Y0);
        Colt dead = new Colt(world.nextId(), X0 + 5, Y0);
        world.addPlayer(alive);
        world.addPlayer(dead);

        dead.takeDamage(100);
        world.update(1 / 60f);

        assertEquals(1, world.snapshot().brawlers().size());
        assertEquals(alive.getId(), world.snapshot().brawlers().get(0).id());
    }

    @Test
    void snapshotDoesNotChangeWhenTheWorldEvolves() {
        GameWorld world = new GameWorld(ARENA);
        Colt colt = new Colt(world.nextId(), X0, Y0);
        world.addPlayer(colt);
        WorldSnapshot before = world.snapshot();

        world.applyInput(colt.getId(), new PlayerInput(1, 0, 1, 0, true));
        world.update(1f);

        assertEquals(X0, before.brawlers().get(0).x());
        assertTrue(before.projectiles().isEmpty());
        assertTrue(world.snapshot().brawlers().get(0).x() > X0);
    }
}
