package io.github.game2d.model;
import io.github.game2d.collision.Circle;

public class Projectile extends GameObject {
    private final Circle hitbox;
    private final int ownerId;
    private final int damage;
    private final float dirX;
    private final float dirY;
    private final float speed;
    private final float maxRange;
    private float distanceTravelled;
    private boolean hit;

    public Projectile(int ownerId, float x, float y, float radius, float dirX, float dirY, float speed, float maxRange, int damage) {
        super(x, y);
        this.hitbox = new Circle(radius);
        this.ownerId = ownerId;
        this.damage = damage;
        this.dirX = dirX;
        this.dirY = dirY;
        this.speed = speed;
        this.maxRange = maxRange;
    }

    @Override
    public Circle getHitbox() {
        return hitbox;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public int getDamage() {
        return damage;
    }

    public void markHit() {
        hit = true;
    }

    @Override
    public void update(float dt) {
        float step = speed * dt;
        x += dirX * step;
        y += dirY * step;
        distanceTravelled += step;
    }

    @Override
    public boolean isExpired() {
        return hit || distanceTravelled >= maxRange;
    }
}
