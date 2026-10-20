package io.github.game2d.model;

import io.github.game2d.model.arena.Arena;

/**
 * Anything that lives in the game world: a position and a radius (a circle), updated at every tick.
 * Each subclass moves itself and handles its own collisions with the scenery in {@link #update}.
 */
public abstract class GameObject {
    private final int id;

    protected float x;
    protected float y;
    protected final float radius;

    /**
     * Creates an object.
     *
     * @param id     unique id inside the world
     * @param x      horizontal position, in tiles
     * @param y      vertical position, in tiles
     * @param radius radius of the hitbox, in tiles
     */
    protected GameObject(int id, float x, float y, float radius) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    /**
     * Advances this object by one step.
     *
     * @param dt    elapsed time, in seconds
     * @param arena the scenery to collide with
     */
    public abstract void update(float dt, Arena arena);

    /** Whether the world must remove this object after the current update. */
    public boolean isExpired() {
        return false;
    }

    /** Returns the unique id of this object in its world. */
    public int getId() {
        return id;
    }

    /** Returns the horizontal position of the center, in tiles. */
    public float getX() {
        return x;
    }

    /** Returns the vertical position of the center, in tiles. */
    public float getY() {
        return y;
    }

    /** Returns the radius of the hitbox, in tiles. */
    public float getRadius() {
        return radius;
    }
}
