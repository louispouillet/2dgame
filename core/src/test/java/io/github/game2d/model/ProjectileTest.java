package io.github.game2d.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.TestArenas;
import org.junit.jupiter.api.Test;

class ProjectileTest {
    private static final Arena ARENA = TestArenas.open(30, 20);
    private static final float X0 = 10f;
    private static final float Y0 = 5f;

    private Projectile projectile(float dirX, float dirY, float speed, float maxRange) {
        return new Projectile(1, 7, X0, Y0, 0.05f, dirX, dirY, speed, maxRange, 15);
    }

    @Test
    void movesAlongItsDirectionAtItsSpeed() {
        Projectile p = projectile(0, 1, 10, 100);

        p.update(0.5f, ARENA);

        assertEquals(X0, p.getX(), 1e-6);
        assertEquals(Y0 + 5, p.getY(), 1e-6);
    }

    @Test
    void exposesOwnerAndDamage() {
        Projectile p = projectile(1, 0, 10, 100);

        assertEquals(7, p.getOwnerId());
        assertEquals(15, p.getDamage());
    }

    @Test
    void isNotExpiredBeforeReachingMaxRange() {
        Projectile p = projectile(1, 0, 10, 4);

        p.update(0.3f, ARENA);

        assertFalse(p.isExpired());
    }

    @Test
    void expiresOnceMaxRangeIsReached() {
        Projectile p = projectile(1, 0, 10, 4);

        p.update(0.4f, ARENA);

        assertTrue(p.isExpired());
    }

    @Test
    void expiresAfterBeingMarkedAsHit() {
        Projectile p = projectile(1, 0, 10, 100);

        p.markHit();

        assertTrue(p.isExpired());
    }

    @Test
    void stopsWhenItReachesAWall() {
        Projectile p = projectile(0, -1, 10, 100);

        p.update(0.5f, ARENA);

        assertTrue(p.isExpired());
    }
}
