package io.github.game2d.model.brawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.model.Projectile;
import io.github.game2d.model.collision.Obstacles;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class BrawlerTest {
    private final AtomicInteger ids = new AtomicInteger();

    @Test
    void shellyAndColtHaveTheirOwnStats() {
        Shelly shelly = new Shelly(1, 0, 0);
        Colt colt = new Colt(2, 0, 0);

        assertEquals(120, shelly.getMaxHealth());
        assertEquals(100, colt.getMaxHealth());
        assertEquals(shelly.getMaxHealth(), shelly.getHealth());
    }

    @Test
    void movesAtItsSpeedInTheGivenDirection() {
        Colt colt = new Colt(1, 0, 0);

        colt.setDirection(1, 0);
        colt.move(1f, Obstacles.NONE);

        assertEquals(5f, colt.getX(), 1e-5);
        assertEquals(0f, colt.getY(), 1e-5);
    }

    @Test
    void diagonalMovementIsNormalized() {
        Colt colt = new Colt(1, 0, 0);

        colt.setDirection(1, 1);
        colt.move(1f, Obstacles.NONE);

        assertEquals(5f, (float) Math.hypot(colt.getX(), colt.getY()), 1e-4);
    }

    @Test
    void zeroDirectionStopsTheBrawler() {
        Colt colt = new Colt(1, 0, 0);
        colt.setDirection(1, 0);

        colt.setDirection(0, 0);
        colt.move(1f, Obstacles.NONE);

        assertEquals(0f, colt.getX(), 1e-6);
    }

    @Test
    void takeDamageReducesHealth() {
        Colt colt = new Colt(1, 0, 0);

        colt.takeDamage(30);

        assertEquals(70, colt.getHealth());
        assertTrue(colt.isAlive());
    }

    @Test
    void healthNeverGoesBelowZero() {
        Colt colt = new Colt(1, 0, 0);

        colt.takeDamage(1000);

        assertEquals(0, colt.getHealth());
        assertFalse(colt.isAlive());
        assertTrue(colt.isExpired());
    }

    @Test
    void negativeDamageIsRejected() {
        Colt colt = new Colt(1, 0, 0);

        assertThrows(IllegalArgumentException.class, () -> colt.takeDamage(-1));
    }

    @Test
    void deadBrawlerDoesNotMove() {
        Colt colt = new Colt(1, 0, 0);
        colt.setDirection(1, 0);
        colt.takeDamage(100);

        colt.move(1f, Obstacles.NONE);

        assertEquals(0f, colt.getX(), 1e-6);
    }

    @Test
    void deadBrawlerCannotAttack() {
        Colt colt = new Colt(1, 0, 0);
        colt.takeDamage(100);

        assertTrue(colt.attack(ids::incrementAndGet, 1, 0).isEmpty());
    }

    @Test
    void attackSpendsAmmoAndSpawnsProjectilesOwnedByTheBrawler() {
        Colt colt = new Colt(4, 0, 0);

        List<Projectile> shots = colt.attack(ids::incrementAndGet, 1, 0);

        assertEquals(1, shots.size());
        assertEquals(4, shots.get(0).getOwnerId());
        assertEquals(colt.getMaxAmmo() - 1, colt.getAmmo());
    }

    @Test
    void doesNotRegenerateRightAfterTakingDamage() {
        Colt colt = new Colt(1, 0, 0);
        colt.takeDamage(50);

        colt.update(2.9f);

        assertEquals(50, colt.getHealth());
    }

    @Test
    void regeneratesAfterTheCombatDelay() {
        Colt colt = new Colt(1, 0, 0);
        colt.takeDamage(50);

        colt.update(3f);
        colt.update(1f);

        assertTrue(colt.getHealth() > 50);
    }

    @Test
    void regenerationNeverExceedsMaxHealth() {
        Colt colt = new Colt(1, 0, 0);
        colt.takeDamage(1);

        for (int i = 0; i < 600; i++) {
            colt.update(0.1f);
        }

        assertEquals(colt.getMaxHealth(), colt.getHealth());
    }

    @Test
    void attackingInterruptsRegeneration() {
        Colt colt = new Colt(1, 0, 0);
        colt.takeDamage(50);
        colt.update(3f);

        colt.attack(ids::incrementAndGet, 1, 0);
        int before = colt.getHealth();
        colt.update(1f);

        assertEquals(before, colt.getHealth());
    }
}
