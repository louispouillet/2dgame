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
        world.addPlayer(shooter, Team.BLUE);
        world.addPlayer(target, Team.RED);

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
        world.addPlayer(shooter, Team.BLUE);

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
        world.addPlayer(shooter, Team.BLUE);

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
        world.addPlayer(shooter, Team.BLUE);

        world.attack(shooter, 1, 0);
        world.update(1 / 60f);

        assertEquals(100, shooter.getHealth());
    }

    @Test
    void deadBrawlerIsRemovedFromWorld() {
        GameWorld world = new GameWorld(ARENA);
        Colt target = new Colt(world.nextId(), X0, Y0);
        world.addPlayer(target, Team.BLUE);

        target.takeDamage(100);
        world.update(1 / 60f);

        assertFalse(world.getGameObjects().contains(target));
    }

    @Test
    void shotMissingTheTargetDoesNoDamage() {
        GameWorld world = new GameWorld(ARENA);
        Colt shooter = new Colt(world.nextId(), X0, Y0);
        Shelly target = new Shelly(world.nextId(), X0 + 2, Y0 + 3);
        world.addPlayer(shooter, Team.BLUE);
        world.addPlayer(target, Team.RED);

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
        world.addPlayer(shooter, Team.BLUE);

        world.attack(shooter, 1, 0);

        long distinct = world.getGameObjects().stream().map(o -> o.getId()).distinct().count();
        assertEquals(world.getGameObjects().size(), distinct);
    }

    @Test
    void deadPlayerStaysRegisteredAfterRemovalFromWorld() {
        GameWorld world = new GameWorld(ARENA);
        Colt p1 = new Colt(world.nextId(), X0, Y0);
        world.addPlayer(p1, Team.BLUE);

        p1.takeDamage(100);
        world.update(1 / 60f);

        assertFalse(world.getGameObjects().contains(p1));
        assertEquals(p1, world.getPlayer(p1.getId()));
    }

    @Test
    void registeringTheSameBrawlerTwiceFails() {
        GameWorld world = new GameWorld(ARENA);
        Colt brawler = new Colt(world.nextId(), X0, Y0);
        world.addPlayer(brawler, Team.BLUE);

        assertThrows(IllegalArgumentException.class, () -> world.addPlayer(brawler, Team.RED));
    }


    @Test
    void projectilePassesThroughAnAllyAndHitsTheEnemyBehind() {
        GameWorld world = new GameWorld(ARENA);
        Colt shooter = new Colt(world.nextId(), X0, Y0);
        Shelly ally = new Shelly(world.nextId(), X0 + 2, Y0);
        Shelly enemy = new Shelly(world.nextId(), X0 + 4, Y0);
        world.addPlayer(shooter, Team.BLUE);
        world.addPlayer(ally, Team.BLUE);
        world.addPlayer(enemy, Team.RED);

        world.attack(shooter, 1, 0);
        for (int i = 0; i < 180; i++) {
            world.update(1 / 60f);
        }

        assertEquals(ally.getMaxHealth(), ally.getHealth());
        assertTrue(enemy.getHealth() < enemy.getMaxHealth());
    }

    @Test
    void playersOfTheSameTeamAreAllies() {
        GameWorld world = new GameWorld(ARENA);
        world.addPlayer(new Colt(1, X0, Y0), Team.BLUE);
        world.addPlayer(new Colt(2, X0 + 1, Y0), Team.BLUE);
        world.addPlayer(new Colt(3, X0 + 2, Y0), Team.RED);

        assertTrue(world.areAllies(1, 2));
        assertTrue(world.areAllies(1, 1));
        assertFalse(world.areAllies(1, 3));
    }

    @Test
    void unknownOrRemovedPlayerHasNoAlly() {
        GameWorld world = new GameWorld(ARENA);
        world.addPlayer(new Colt(1, X0, Y0), Team.BLUE);
        world.addPlayer(new Colt(2, X0 + 1, Y0), Team.BLUE);

        world.removePlayer(2);

        assertFalse(world.areAllies(1, 2));
        assertFalse(world.areAllies(9, 9));
    }

    @Test
    void aliveCountOnlyCountsLivingPlayersOfTheTeam() {
        GameWorld world = new GameWorld(ARENA);
        Colt blue = new Colt(1, X0, Y0);
        Colt deadBlue = new Colt(2, X0 + 1, Y0);
        world.addPlayer(blue, Team.BLUE);
        world.addPlayer(deadBlue, Team.BLUE);
        world.addPlayer(new Colt(3, X0 + 2, Y0), Team.RED);

        deadBlue.takeDamage(deadBlue.getHealth());

        assertEquals(1, world.aliveCount(Team.BLUE));
        assertEquals(1, world.aliveCount(Team.RED));
    }
}
