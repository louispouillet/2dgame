package io.github.game2d.model.weapon;

import io.github.game2d.model.Projectile;

public class Shotgun extends Weapon {
    private static final int MAX_AMMO = 3;
    private static final float COOLDOWN = 0.6f;
    private static final float RELOAD_TIME = 1.5f;
    private static final float MAX_RANGE = 4f;
    private static final int NUM_PROJECTILES = 5;
    private static final float SPREAD_ANGLE = 7;
    private static final float PROJECTILE_RADIUS = 0.1f;
    private static final float PROJECTILE_SPEED = 10f;
    private static final int DAMAGE = 15;

    public Shotgun() {
        super(MAX_AMMO, COOLDOWN, RELOAD_TIME, new SpreadPattern(NUM_PROJECTILES, SPREAD_ANGLE));
    }

    @Override
    protected Projectile createProjectile(int id, int ownerId, float x, float y, float dirX, float dirY) {
        return new Projectile(id, ownerId, x, y, PROJECTILE_RADIUS, dirX, dirY, PROJECTILE_SPEED, MAX_RANGE, DAMAGE);
    }
}
