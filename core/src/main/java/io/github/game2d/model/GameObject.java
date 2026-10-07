package io.github.game2d.model;

public abstract class GameObject {
    private final int id;

    protected float x;
    protected float y;
    protected final float radius;

    protected GameObject(int id, float x, float y, float radius) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public abstract void update(float dt);

    public boolean isExpired() {
        return false;
    }

    public int getId() {
        return id;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getRadius() {
        return radius;
    }
}
