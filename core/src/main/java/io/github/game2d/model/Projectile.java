package io.github.game2d.model;

public class Projectile extends GameObject {
    private final float dirX;
    private final float dirY;
    private final float speed;
    private final float maxRange;
    private float distanceTravelled;

    public Projectile(float x, float y, float width, float height, float dirX, float dirY, float speed, float maxRange) {
        super(x, y, width, height);
        this.dirX = dirX;
        this.dirY = dirY;
        this.speed = speed;
        this.maxRange = maxRange;
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
        return distanceTravelled >= maxRange;
    }
}
