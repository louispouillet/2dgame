package io.github.game2d.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.ArenaLoader;
import io.github.game2d.model.brawler.Colt;
import org.junit.jupiter.api.Test;

class GameWorldVisibilityTest {
    private static final Arena ARENA = ArenaLoader.parse("""
        #########
        #1.....2#
        #.......#
        #..~~~..#
        #.......#
        #########
        """);

    private final GameWorld world = new GameWorld(ARENA);

    @Test
    void enemyInBushIsHiddenWhenFar() {
        Colt viewer = new Colt(1, 1.5f, 1.5f);
        Colt hidden = new Colt(2, 4.5f, 2.5f);

        assertFalse(world.isVisibleTo(hidden, viewer));
    }

    @Test
    void enemyInBushIsVisibleWhenClose() {
        Colt viewer = new Colt(1, 4.5f, 4.4f);
        Colt hidden = new Colt(2, 4.5f, 2.5f);

        assertTrue(world.isVisibleTo(hidden, viewer));
    }

    @Test
    void enemyOutsideBushIsVisibleWhenFar() {
        Colt viewer = new Colt(1, 7.5f, 1.5f);
        Colt enemy = new Colt(2, 1.5f, 4.5f);

        assertTrue(world.isVisibleTo(enemy, viewer));
    }

    @Test
    void brawlerInBushSeesHimself() {
        Colt hidden = new Colt(1, 4.5f, 2.5f);

        assertTrue(world.isVisibleTo(hidden, hidden));
    }

    @Test
    void enemyInBushIsHiddenWithoutViewer() {
        Colt hidden = new Colt(1, 4.5f, 2.5f);

        assertFalse(world.isVisibleTo(hidden, null));
    }

    @Test
    void allyInBushStaysVisibleWhenFar() {
        Colt viewer = new Colt(1, 1.5f, 1.5f);
        Colt ally = new Colt(2, 4.5f, 2.5f);
        world.addPlayer(viewer, Team.BLUE);
        world.addPlayer(ally, Team.BLUE);

        assertTrue(world.isVisibleTo(ally, viewer));
    }

    @Test
    void enemyInBushIsRevealedWhenItShoots() {
        Colt viewer = new Colt(1, 1.5f, 1.5f);
        Colt enemy = new Colt(2, 4.5f, 2.5f);
        world.addPlayer(viewer, Team.BLUE);
        world.addPlayer(enemy, Team.RED);

        world.attack(enemy, -1, 0);

        assertTrue(world.isVisibleTo(enemy, viewer));
    }

    @Test
    void enemyInBushStaysRevealedThroughoutItsBurst() {
        Colt viewer = new Colt(1, 1.5f, 1.5f);
        Colt enemy = new Colt(2, 4.5f, 2.5f);
        world.addPlayer(viewer, Team.BLUE);
        world.addPlayer(enemy, Team.RED);

        world.attack(enemy, -1, 0);
        // the burst lasts 0.5 s: this is well after the first bullet and before the last one
        for (int i = 0; i < 9; i++) {
            world.update(0.05f);
        }
        assertTrue(world.isVisibleTo(enemy, viewer));

        for (int i = 0; i < 20; i++) {
            world.update(0.05f);
        }
        assertFalse(world.isVisibleTo(enemy, viewer));
    }

    @Test
    void enemyInBushIsRevealedWhenHit() {
        Colt viewer = new Colt(1, 1.5f, 1.5f);
        Colt enemy = new Colt(2, 4.5f, 2.5f);
        world.addPlayer(viewer, Team.BLUE);
        world.addPlayer(enemy, Team.RED);

        enemy.takeDamage(10);

        assertTrue(world.isVisibleTo(enemy, viewer));
    }

    @Test
    void enemyInBushIsHiddenAgainAfterTheReveal() {
        Colt viewer = new Colt(1, 1.5f, 1.5f);
        Colt enemy = new Colt(2, 4.5f, 2.5f);
        world.addPlayer(viewer, Team.BLUE);
        world.addPlayer(enemy, Team.RED);
        enemy.takeDamage(10);

        for (int i = 0; i < 20; i++) {
            world.update(0.05f);
        }

        assertFalse(world.isVisibleTo(enemy, viewer));
    }

    @Test
    void registeredEnemyInBushIsStillHiddenWhenFar() {
        Colt viewer = new Colt(1, 1.5f, 1.5f);
        Colt enemy = new Colt(2, 4.5f, 2.5f);
        world.addPlayer(viewer, Team.BLUE);
        world.addPlayer(enemy, Team.RED);

        assertFalse(world.isVisibleTo(enemy, viewer));
    }
}
