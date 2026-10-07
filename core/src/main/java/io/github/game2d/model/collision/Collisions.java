package io.github.game2d.model.collision;

import io.github.game2d.model.GameObject;
import io.github.game2d.model.Projectile;

public final class Collisions {
    private Collisions() {}

    public static boolean overlaps(GameObject a, GameObject b) {
        float ax = a.getX();
        float ay = a.getY();
        float bx = b.getX();
        float by = b.getY();
        return switch (a.getHitbox()) {
            case Circle c1 -> switch (b.getHitbox()) {
                case Circle c2 -> circleCircle(c1, ax, ay, c2, bx, by);
                case Rect r2 -> circleRect(c1, ax, ay, r2, bx, by);
            };
            case Rect r1 -> switch (b.getHitbox()) {
                case Circle c2 -> circleRect(c2, bx, by, r1, ax, ay);
                case Rect r2 -> rectRect(r1, ax, ay, r2, bx, by);
            };
        };
    }

    public static boolean sweptOverlaps(Projectile projectile, GameObject target) {
        return switch (target.getHitbox()) {
            case Circle c -> (segmentHitsCircle(
                projectile.getPreviousX(), projectile.getPreviousY(),
                projectile.getX(), projectile.getY(),
                target.getX(), target.getY(),
                c.radius() + projectile.getHitbox().radius())
            );
            case Rect r -> (false);
        };
    }

    private static boolean segmentHitsCircle(float fromX, float fromY, float toX, float toY, float targetX, float targetY, float radius) {
        float dx = toX - fromX;
        float dy = toY - fromY;
        float lengthSquared = dx * dx + dy * dy;
        float t = lengthSquared == 0 ? 0 : Math.clamp(((targetX - fromX) * dx + (targetY - fromY) * dy) / lengthSquared, 0f, 1f);
        float nearestX = fromX + t * dx;
        float nearestY = fromY + t * dy;
        float ex = targetX - nearestX;
        float ey = targetY - nearestY;
        return ex * ex + ey * ey <= radius * radius;
    }

    private static boolean circleCircle(Circle a, float ax, float ay, Circle b, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        float r = a.radius() + b.radius();
        return dx * dx + dy * dy <= r * r;
    }

    private static boolean circleRect(Circle c, float cx, float cy, Rect r, float rx, float ry) {
        float nearestX = Math.clamp(cx, rx - r.width() / 2, rx + r.width() / 2);
        float nearestY = Math.clamp(cy, ry - r.height() / 2, ry + r.height() / 2);
        float dx = cx - nearestX;
        float dy = cy - nearestY;
        return dx * dx + dy * dy <= c.radius() * c.radius();
    }

    private static boolean rectRect(Rect a, float ax, float ay, Rect b, float bx, float by) {
        return Math.abs(ax - bx) <= (a.width() + b.width()) / 2
            && Math.abs(ay - by) <= (a.height() + b.height()) / 2;
    }
}
