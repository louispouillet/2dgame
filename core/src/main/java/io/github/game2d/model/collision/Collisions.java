package io.github.game2d.model.collision;

import io.github.game2d.model.GameObject;

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

    public static boolean sweptOverlaps(float fromX, float fromY, float toX, float toY, float radius, GameObject target) {

        return true;
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
