package io.github.game2d.model.collision;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.model.Projectile;
import io.github.game2d.model.brawler.Colt;
import org.junit.jupiter.api.Test;

class CollisionsTest {
    private static Projectile shotFromOriginToTheRight() {
        Projectile projectile = new Projectile(1, 99, 0f, 0f, 0.05f, 1f, 0f, 12f, 20f, 10);
        projectile.update(1f);
        return projectile;
    }

    @Test
    void shotHitsATargetItPassedThroughBetweenTwoTicks() {
        assertTrue(Collisions.sweptOverlaps(shotFromOriginToTheRight(), new Colt(2, 6f, 0f)));
    }

    @Test
    void shotMissesATargetBesideItsPath() {
        assertFalse(Collisions.sweptOverlaps(shotFromOriginToTheRight(), new Colt(2, 6f, 1f)));
    }

    @Test
    void shotHitsATargetAtTheEndOfItsPath() {
        assertTrue(Collisions.sweptOverlaps(shotFromOriginToTheRight(), new Colt(2, 12.2f, 0f)));
    }

    @Test
    void circleTouchesBoxWhenItOverlapsASide() {
        assertTrue(Collisions.circleTouchesBox(1.1f, 0.5f, 0.25f, 0f, 0f, 1f, 1f));
    }

    @Test
    void circleExactlyAgainstABoxDoesNotTouchIt() {
        assertFalse(Collisions.circleTouchesBox(1.25f, 0.5f, 0.25f, 0f, 0f, 1f, 1f));
    }

    @Test
    void circleNearACornerDoesNotTouchTheBox() {
        assertFalse(Collisions.circleTouchesBox(1.2f, 1.2f, 0.25f, 0f, 0f, 1f, 1f));
    }

    @Test
    void circleInsideTheBoxTouchesIt() {
        assertTrue(Collisions.circleTouchesBox(0.5f, 0.5f, 0.1f, 0f, 0f, 1f, 1f));
    }
}
