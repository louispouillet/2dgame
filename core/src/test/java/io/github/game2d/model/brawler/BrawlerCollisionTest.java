package io.github.game2d.model.brawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.ArenaLoader;
import org.junit.jupiter.api.Test;

class BrawlerCollisionTest {
    private static final Arena ARENA = ArenaLoader.parse("""
        #######
        #1...2#
        #..W..#
        #....~#
        #######
        """);
    private static final float TICK = 1 / 30f;
    private static final float RADIUS = 0.25f;

    private static void moveFor(Colt colt, float seconds) {
        for (float time = 0; time < seconds; time += TICK) {
            colt.update(TICK, ARENA);
        }
    }

    @Test
    void stopsAgainstAWall() {
        Colt colt = new Colt(1, 1.5f, 1.5f);
        colt.setDirection(-1, 0);

        moveFor(colt, 1f);

        assertTrue(colt.getX() >= 1 + RADIUS);
        assertTrue(colt.getX() < 1 + RADIUS + 0.02f);
    }

    @Test
    void cannotLeaveTheArena() {
        Colt colt = new Colt(1, 1.5f, 1.5f);
        colt.setDirection(0, -1);

        moveFor(colt, 1f);

        assertTrue(colt.getY() >= 1 + RADIUS);
    }

    @Test
    void slidesAlongAWallWhenMovingDiagonally() {
        Colt colt = new Colt(1, 1.5f, 1.5f);
        colt.setDirection(-1, 1);

        moveFor(colt, 0.5f);

        assertTrue(colt.getX() < 1 + RADIUS + 0.02f);
        assertTrue(colt.getY() > 2.9f);
    }

    @Test
    void stopsAgainstWater() {
        Colt colt = new Colt(1, 2.5f, 2.5f);
        colt.setDirection(1, 0);

        moveFor(colt, 1f);

        assertTrue(colt.getX() <= 3 - RADIUS);
        assertTrue(colt.getX() > 3 - RADIUS - 0.02f);
    }

    @Test
    void walksThroughABush() {
        Colt colt = new Colt(1, 3.5f, 1.5f);
        colt.setDirection(1, 0);

        moveFor(colt, 0.4f);

        assertEquals(5.5f, colt.getX(), 0.2f);
    }

    @Test
    void resyncsToAFreePosition() {
        Colt colt = new Colt(1, 1.5f, 1.5f);

        colt.correctPositionTo(4.5f, 3.5f, ARENA);

        assertEquals(4.5f, colt.getX());
        assertEquals(3.5f, colt.getY());
    }

    @Test
    void cannotResyncInsideAWall() {
        Colt colt = new Colt(1, 1.5f, 1.5f);

        assertThrows(IllegalArgumentException.class, () -> colt.correctPositionTo(0.5f, 1.5f, ARENA));
    }

    @Test
    void cannotResyncInsideWater() {
        Colt colt = new Colt(1, 1.5f, 1.5f);

        assertThrows(IllegalArgumentException.class, () -> colt.correctPositionTo(3.5f, 2.5f, ARENA));
    }

    @Test
    void cannotResyncWithTheCircleTouchingAWall() {
        Colt colt = new Colt(1, 1.5f, 1.5f);

        // Le centre est dans une case libre, mais le cercle déborde de RADIUS / 2 sur le mur.
        assertThrows(IllegalArgumentException.class, () -> colt.correctPositionTo(1 + RADIUS / 2, 1.5f, ARENA));
    }

    @Test
    void cannotResyncOutsideTheArena() {
        Colt colt = new Colt(1, 1.5f, 1.5f);

        assertThrows(IllegalArgumentException.class, () -> colt.correctPositionTo(-1f, 1.5f, ARENA));
    }

    @Test
    void keepsItsPositionWhenAResyncIsRefused() {
        Colt colt = new Colt(1, 1.5f, 1.5f);

        assertThrows(IllegalArgumentException.class, () -> colt.correctPositionTo(0.5f, 1.5f, ARENA));

        assertEquals(1.5f, colt.getX());
        assertEquals(1.5f, colt.getY());
    }

    @Test
    void resyncsToAPositionInABush() {
        Colt colt = new Colt(1, 1.5f, 1.5f);

        colt.correctPositionTo(5.5f, 3.5f, ARENA);

        assertEquals(5.5f, colt.getX());
    }
}
