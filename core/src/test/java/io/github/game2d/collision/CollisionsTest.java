package io.github.game2d.collision;
import io.github.game2d.model.GameObject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CollisionsTest {
    @Test
    void circlesOverlapWhenCloserThanSumOfRadii() {
        assertTrue(Collisions.overlaps(at(0, 0, new Circle(1)), at(1.5f, 0, new Circle(1))));
    }

    @Test
    void circlesTouchingCount() {
        assertTrue(Collisions.overlaps(at(0, 0, new Circle(1)), at(2, 0, new Circle(1))));
    }

    @Test
    void distantCirclesDoNotOverlap() {
        assertFalse(Collisions.overlaps(at(0, 0, new Circle(1)), at(3, 0, new Circle(1))));
    }

    @Test
    void circleOverlapsRectFromSide() {
        assertTrue(Collisions.overlaps(at(0, 0, new Circle(1)), at(1.5f, 0, new Rect(2, 10))));
    }

    @Test
    void circleDoesNotOverlapRectCornerEvenInsideBoundingBox() {
        // coin du rect en (1,1) ; distance au centre du cercle = sqrt(2) > 1
        assertFalse(Collisions.overlaps(at(0, 0, new Circle(1)), at(2, 2, new Rect(2, 2))));
    }

    @Test
    void circleRectIsSymmetric() {
        Circle c = new Circle(1);
        Rect r = new Rect(2, 10);
        assertTrue(Collisions.overlaps(at(1.5f, 0, r), at(0, 0, c)));
        assertFalse(Collisions.overlaps(at(5, 0, r), at(0, 0, c)));
    }

    @Test
    void rectsOverlapOnlyWhenBothAxesOverlap() {
        Rect r = new Rect(2, 2);
        assertTrue(Collisions.overlaps(at(0, 0, r), at(1.5f, 1.5f, r)));
        assertFalse(Collisions.overlaps(at(0, 0, r), at(1.5f, 3, r)));
    }

    private static GameObject at(float x, float y, Hitbox hitbox) {
        return new GameObject(x, y) {
            @Override
            public void update(float dt) {}

            @Override
            public Hitbox getHitbox() {
                return hitbox;
            }
        };
    }
}
