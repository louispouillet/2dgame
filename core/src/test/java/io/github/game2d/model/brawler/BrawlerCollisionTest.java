package io.github.game2d.model.brawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            colt.move(TICK, ARENA);
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
}
