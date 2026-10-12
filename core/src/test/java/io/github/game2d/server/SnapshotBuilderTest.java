package io.github.game2d.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.game2d.model.GameWorld;
import io.github.game2d.model.Projectile;
import io.github.game2d.model.Team;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.ArenaLoader;
import io.github.game2d.model.arena.TestArenas;
import io.github.game2d.model.brawler.Colt;
import io.github.game2d.model.brawler.Shelly;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class SnapshotBuilderTest {
    private static final Arena ARENA = TestArenas.open(30, 20);
    private static final float X0 = 10f;
    private static final float Y0 = 10f;
    private static final Arena BUSH_ARENA = ArenaLoader.parse("""
        #########
        #1.....2#
        #.......#
        #..~~~..#
        #.......#
        #########
        """);

    private static boolean sees(WorldSnapshot snapshot, Colt brawler) {
        return snapshot.brawlers().stream().anyMatch(state -> state.id() == brawler.getId());
    }

    @Test
    void snapshotOmitsEnemiesHiddenFromTheViewer() {
        GameWorld world = new GameWorld(BUSH_ARENA);
        Colt viewer = new Colt(world.nextId(), 1.5f, 1.5f);
        Colt hidden = new Colt(world.nextId(), 4.5f, 2.5f);
        world.addPlayer(viewer, Team.BLUE);
        world.addPlayer(hidden, Team.RED);

        WorldSnapshot snapshot = new SnapshotBuilder(world).build(0, id -> 0, viewer.getId());

        assertFalse(sees(snapshot, hidden));
    }

    @Test
    void snapshotKeepsAlliesHiddenInBushes() {
        GameWorld world = new GameWorld(BUSH_ARENA);
        Colt viewer = new Colt(world.nextId(), 1.5f, 1.5f);
        Colt ally = new Colt(world.nextId(), 4.5f, 2.5f);
        world.addPlayer(viewer, Team.BLUE);
        world.addPlayer(ally, Team.BLUE);

        WorldSnapshot snapshot = new SnapshotBuilder(world).build(0, id -> 0, viewer.getId());

        assertTrue(sees(snapshot, ally));
    }

    @Test
    void projectilesOfAHiddenEnemyStayVisible() {
        GameWorld world = new GameWorld(BUSH_ARENA);
        Colt viewer = new Colt(world.nextId(), 1.5f, 1.5f);
        Colt hidden = new Colt(world.nextId(), 4.5f, 2.5f);
        world.addPlayer(viewer, Team.BLUE);
        world.addPlayer(hidden, Team.RED);

        world.attack(hidden, 1, 0);
        WorldSnapshot snapshot = new SnapshotBuilder(world).build(0, id -> 0, viewer.getId());

        assertFalse(sees(snapshot, hidden));
        assertFalse(snapshot.projectiles().isEmpty());
    }

    @Test
    void snapshotContainsEveryLivingBrawler() {
        GameWorld world = new GameWorld(ARENA);
        Colt colt = new Colt(world.nextId(), X0 + 1, Y0 + 2);
        Shelly shelly = new Shelly(world.nextId(), X0 + 3, Y0 + 4);
        world.addPlayer(colt, Team.BLUE);
        world.addPlayer(shelly, Team.RED);

        WorldSnapshot snapshot = new SnapshotBuilder(world).build(0, id -> 0, 1);

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
        world.addPlayer(shooter, Team.BLUE);

        world.attack(shooter, 1, 0);
        WorldSnapshot snapshot = new SnapshotBuilder(world).build(0, id -> 0, 1);

        List<Projectile> projectiles = world.getGameObjects().stream()
            .filter(o -> o instanceof Projectile)
            .map(o -> (Projectile) o)
            .toList();
        assertEquals(projectiles.size(), snapshot.projectiles().size());
        assertEquals(projectiles.getFirst().getRadius(), snapshot.projectiles().getFirst().radius());
    }

    @Test
    void snapshotOmitsDeadBrawlers() {
        GameWorld world = new GameWorld(ARENA);
        SnapshotBuilder builder = new SnapshotBuilder(world);
        Colt alive = new Colt(world.nextId(), X0, Y0);
        Colt dead = new Colt(world.nextId(), X0 + 5, Y0);
        world.addPlayer(alive, Team.BLUE);
        world.addPlayer(dead, Team.RED);

        dead.takeDamage(100);
        world.update(1 / 60f);

        assertEquals(1, builder.build(0, id -> 0, 1).brawlers().size());
        assertEquals(alive.getId(), builder.build(0, id -> 0, 1).brawlers().get(0).id());
    }

    @Test
    void snapshotDoesNotChangeWhenTheWorldEvolves() {
        GameWorld world = new GameWorld(ARENA);
        SnapshotBuilder builder = new SnapshotBuilder(world);
        InputApplier applier = new InputApplier(world);
        Colt colt = new Colt(world.nextId(), X0, Y0);
        world.addPlayer(colt, Team.BLUE);
        WorldSnapshot before = builder.build(0, id -> 0, 1);

        applier.apply(colt.getId(), new PlayerInput(1, 1, 0, 1, 0, true));
        world.update(1f);

        assertEquals(X0, before.brawlers().get(0).x());
        assertTrue(before.projectiles().isEmpty());
        assertTrue(builder.build(0, id -> 0, 1).brawlers().get(0).x() > X0);
    }
}
