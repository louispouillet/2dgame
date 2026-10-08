package io.github.game2d.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.game2d.model.GameWorld;
import io.github.game2d.model.Projectile;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.TestArenas;
import io.github.game2d.model.brawler.Colt;
import io.github.game2d.protocol.PlayerInput;
import java.util.List;
import org.junit.jupiter.api.Test;

class InputApplierTest {
    private static final float DELTA = 1e-4f;
    private static final Arena ARENA = TestArenas.open(30, 20);
    private static final float X0 = 10f;
    private static final float Y0 = 10f;

    @Test
    void inputIsRoutedToThePlayersBrawler() {
        GameWorld world = new GameWorld(ARENA);
        InputApplier applier = new InputApplier(world);
        Colt p1 = new Colt(world.nextId(), X0, Y0);
        Colt p2 = new Colt(world.nextId(), X0 + 5, Y0);
        world.addPlayer(p1);
        world.addPlayer(p2);

        applier.apply(p2.getId(), new PlayerInput(1, 1, 0, 1, 0, false));
        world.update(1f);

        assertEquals(X0, p1.getX());
        assertTrue(p2.getX() > X0 + 5);
    }

    @Test
    void unknownBrawlerIsIgnored() {
        GameWorld world = new GameWorld(ARENA);
        InputApplier applier = new InputApplier(world);

        applier.apply(42, new PlayerInput(1, 1, 0, 1, 0, true));

        assertTrue(world.getGameObjects().isEmpty());
    }

    private static List<Projectile> projectilesOf(GameWorld world) {
        return world.getGameObjects().stream()
            .filter(o -> o instanceof Projectile)
            .map(o -> (Projectile) o)
            .toList();
    }

    private static Projectile shootOnce(float aimX, float aimY) {
        GameWorld world = new GameWorld(ARENA);
        InputApplier applier = new InputApplier(world);
        Colt shooter = new Colt(world.nextId(), X0, Y0);
        world.addPlayer(shooter);

        applier.apply(shooter.getId(), new PlayerInput(1, 0, 0, aimX, aimY, true));
        world.update(0.1f);

        return projectilesOf(world).getFirst();
    }

    @Test
    void aimIsAppliedOnBothAxes() {
        Projectile shot = shootOnce(0, 1);

        assertEquals(X0, shot.getX(), DELTA);
        assertTrue(shot.getY() > Y0);
    }

    @Test
    void aimLengthDoesNotChangeTheProjectileSpeed() {
        Projectile normal = shootOnce(1, 0);
        Projectile huge = shootOnce(100, 0);

        assertEquals(normal.getX(), huge.getX(), DELTA);
        assertEquals(normal.getY(), huge.getY(), DELTA);
    }

    @Test
    void nanAimDoesNotShoot() {
        GameWorld world = new GameWorld(ARENA);
        InputApplier applier = new InputApplier(world);
        Colt shooter = new Colt(world.nextId(), X0, Y0);
        world.addPlayer(shooter);

        applier.apply(shooter.getId(), new PlayerInput(1, 0, 0, Float.NaN, 1, true));

        assertTrue(projectilesOf(world).isEmpty());
    }

    @Test
    void zeroAimDoesNotShoot() {
        GameWorld world = new GameWorld(ARENA);
        InputApplier applier = new InputApplier(world);
        Colt shooter = new Colt(world.nextId(), X0, Y0);
        world.addPlayer(shooter);

        applier.apply(shooter.getId(), new PlayerInput(1, 0, 0, 0, 0, true));

        assertTrue(projectilesOf(world).isEmpty());
    }

    @Test
    void nanMovementDoesNotCorruptThePosition() {
        GameWorld world = new GameWorld(ARENA);
        InputApplier applier = new InputApplier(world);
        Colt player = new Colt(world.nextId(), X0, Y0);
        world.addPlayer(player);

        applier.apply(player.getId(), new PlayerInput(1, Float.NaN, Float.POSITIVE_INFINITY, 1, 0, false));
        world.update(1f);

        assertEquals(X0, player.getX());
        assertEquals(Y0, player.getY());
    }

    @Test
    void hugeMovementIsNotFasterThanNormalMovement() {
        GameWorld normalWorld = new GameWorld(ARENA);
        Colt normal = new Colt(normalWorld.nextId(), X0, Y0);
        normalWorld.addPlayer(normal);
        new InputApplier(normalWorld).apply(normal.getId(), new PlayerInput(1, 1, 0, 1, 0, false));
        normalWorld.update(0.1f);

        GameWorld hugeWorld = new GameWorld(ARENA);
        Colt huge = new Colt(hugeWorld.nextId(), X0, Y0);
        hugeWorld.addPlayer(huge);
        new InputApplier(hugeWorld).apply(huge.getId(), new PlayerInput(1, 1000, 0, 1, 0, false));
        hugeWorld.update(0.1f);

        assertEquals(normal.getX(), huge.getX(), DELTA);
    }
}
