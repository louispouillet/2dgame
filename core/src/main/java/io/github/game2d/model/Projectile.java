package io.github.game2d.model;

public class Projectile extends GameObject {
    private final float dirX;
    private final float dirY;
    private final float speed;

    public Projectile(float x, float y, float width, float height, float dirX, float dirY, float speed) {
        super(x, y, width, height);
        this.dirX = dirX;
        this.dirY = dirY;
        this.speed = speed;
    }

    @Override
    public void update(float dt) {
        x += dirX * speed * dt;
        y += dirY * speed * dt;
    }
}