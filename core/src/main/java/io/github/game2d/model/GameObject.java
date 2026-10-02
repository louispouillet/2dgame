package io.github.game2d.model;
import io.github.game2d.collision.Hitbox;

public abstract class GameObject {
    private static int nextId = 1;
    private final int id;

    protected float x;
    protected float y;

    protected GameObject(float x, float y) {
        this.id = nextId++;
        this.x = x;
        this.y = y;
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

    public abstract Hitbox getHitbox();
}
