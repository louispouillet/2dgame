package io.github.game2d.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProjectileTest {
    private Projectile projectile(float dirX, float dirY, float speed, float maxRange) {
        return new Projectile(1, 7, 0, 0, 0.05f, dirX, dirY, speed, maxRange, 15);
    }

    @Test
    void movesAlongItsDirectionAtItsSpeed() {
        Projectile p = projectile(0, 1, 10, 100);

        p.update(0.5f);

        assertEquals(0, p.getX(), 1e-6);
        assertEquals(5, p.getY(), 1e-6);
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

        p.update(0.3f);

        assertFalse(p.isExpired());
    }

    @Test
    void expiresOnceMaxRangeIsReached() {
        Projectile p = projectile(1, 0, 10, 4);

        p.update(0.4f);

        assertTrue(p.isExpired());
    }

    @Test
    void expiresAfterBeingMarkedAsHit() {
        Projectile p = projectile(1, 0, 10, 100);

        p.markHit();

        assertTrue(p.isExpired());
    }
}
