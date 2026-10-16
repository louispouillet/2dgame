package io.github.game2d.model;

import io.github.game2d.model.arena.Arena;

public class Projectile extends GameObject {
    private final int ownerId;
    private final int damage;
    private final float dirX;
    private final float dirY;
    private final float speed;
    private final float maxRange;
    private float distanceTravelled;
    private float previousX;
    private float previousY;
    private boolean hit;

    public Projectile(int id, int ownerId, float x, float y, float radius, float dirX, float dirY, float speed, float maxRange, int damage) {
        super(id, x, y, radius);
        this.previousX = x;
        this.previousY = y;
        this.ownerId = ownerId;
        this.damage = damage;
        this.dirX = dirX;
        this.dirY = dirY;
        this.speed = speed;
        this.maxRange = maxRange;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public float getDirX() {
        return dirX;
    }

    public float getDirY() {
        return dirY;
    }

    public int getDamage() {
        return damage;
    }

    public float getPreviousX() {
        return previousX;
    }

    public float getPreviousY() {
        return previousY;
    }

    public void markHit() {
        hit = true;
    }

    @Override
    public void update(float dt, Arena arena) {
        previousX = x;
        previousY = y;
        float distance = speed * dt;
        x += dirX * distance;
        y += dirY * distance;
        distanceTravelled += distance;
        if (arena.blocksShots(x, y, radius)) {
            hit = true;
        }
    }

    @Override
    public boolean isExpired() {
        return hit || distanceTravelled >= maxRange;
    }
}
