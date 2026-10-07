package io.github.game2d.model.collision;

import io.github.game2d.model.GameObject;
import io.github.game2d.model.Projectile;

public final class Collisions {
    private Collisions() {}

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

    public static boolean circleTouchesBox(float centerX, float centerY, float radius, float left, float bottom, float right, float top) {
        float nearestX = Math.clamp(centerX, left, right);
        float nearestY = Math.clamp(centerY, bottom, top);
        float dx = centerX - nearestX;
        float dy = centerY - nearestY;
        return dx * dx + dy * dy < radius * radius;
    }
}
