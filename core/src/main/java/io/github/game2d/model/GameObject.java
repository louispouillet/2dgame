package io.github.game2d.model;

public abstract class GameObject {
    private static int nextId = 1;
    private final int id;

    protected float x;
    protected float y;
    protected final float width;
    protected final float height;

    protected GameObject(float x, float y, float width, float height) {
        this.id = nextId++;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
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
    public float getWidth() {
        return width;
    }
    public float getHeight() {
        return height;
    }
}
