package io.github.game2d.model.collision;

import io.github.game2d.model.GameObject;
import io.github.game2d.model.Projectile;

/** Geometry tests shared by the game rules. All shapes are circles or boxes. */
public final class Collisions {
    private Collisions() {}

    /**
     * Whether a projectile touched an object anywhere along the path it travelled during the last update.
     * This avoids fast bullets passing through a target between two ticks.
     */
    public static boolean sweptOverlaps(Projectile projectile, GameObject target) {
        float fromX = projectile.getPreviousX();
        float fromY = projectile.getPreviousY();
        float dx = projectile.getX() - fromX;
        float dy = projectile.getY() - fromY;
        float lengthSquared = dx * dx + dy * dy;
        float t = lengthSquared == 0 ? 0 : Math.clamp(((target.getX() - fromX) * dx + (target.getY() - fromY) * dy) / lengthSquared, 0f, 1f);
        float ex = target.getX() - (fromX + t * dx);
        float ey = target.getY() - (fromY + t * dy);
        float radius = target.getRadius() + projectile.getRadius();
        return ex * ex + ey * ey <= radius * radius;
    }

    /**
     * Whether a circle overlaps an axis-aligned box.
     *
     * @param left   smallest x of the box
     * @param bottom smallest y of the box
     * @param right  largest x of the box
     * @param top    largest y of the box
     */
    public static boolean circleTouchesBox(float centerX, float centerY, float radius, float left, float bottom, float right, float top) {
        float nearestX = Math.clamp(centerX, left, right);
        float nearestY = Math.clamp(centerY, bottom, top);
        float dx = centerX - nearestX;
        float dy = centerY - nearestY;
        return dx * dx + dy * dy < radius * radius;
    }
}
