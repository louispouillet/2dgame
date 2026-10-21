package io.github.game2d.model;

import io.github.game2d.model.arena.Arena;

/**
 * A bullet: it flies in a straight line and disappears when it hits a wall, a brawler, or after its maximum range.
 */
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

    /**
     * Creates a projectile.
     *
     * @param ownerId id of the brawler that fired it
     * @param dirX    horizontal component of the direction (unit vector)
     * @param dirY    vertical component of the direction (unit vector)
     * @param speed   tiles per second
     * @param maxRange distance after which it disappears, in tiles
     * @param damage  health points removed from the brawler it hits
     */
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

    /** Returns the id of the brawler that fired this projectile. */
    public int getOwnerId() {
        return ownerId;
    }

    /** Returns the horizontal component of the direction. */
    public float getDirX() {
        return dirX;
    }

    /** Returns the vertical component of the direction. */
    public float getDirY() {
        return dirY;
    }

    /** Returns the health points removed on impact. */
    public int getDamage() {
        return damage;
    }

    /** Returns the horizontal position before the last update (start of the swept path). */
    public float getPreviousX() {
        return previousX;
    }

    /** Returns the vertical position before the last update (start of the swept path). */
    public float getPreviousY() {
        return previousY;
    }

    /** Marks this projectile as having hit something: it expires at the end of the tick. */
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
