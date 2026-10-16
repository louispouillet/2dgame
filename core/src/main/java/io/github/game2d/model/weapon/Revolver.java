package io.github.game2d.model.weapon;

import io.github.game2d.model.Projectile;

public class Revolver extends Weapon {
    private static final int MAX_AMMO = 3;
    // Must last at least the whole burst ((BULLETS_PER_BURST - 1) * BULLET_INTERVAL):
    // a new shot during the burst would cut the remaining bullets.
    private static final float COOLDOWN = 1f;
    private static final float RELOAD_TIME = 2f;
    private static final float MAX_RANGE = 7f;
    private static final float PROJECTILE_RADIUS = 0.1f;
    private static final float PROJECTILE_SPEED = 9f;
    private static final int DAMAGE = 10;
    private static final int BULLETS_PER_BURST = 6;
    private static final float BULLET_INTERVAL = 0.1f;
    private static final float GUN_OFFSET = 0.12f;
    private static final float MUZZLE_DISTANCE = 0.5f;

    public Revolver() {
        super(MAX_AMMO, COOLDOWN, RELOAD_TIME, new BurstPattern(BULLETS_PER_BURST, BULLET_INTERVAL, GUN_OFFSET),
            MUZZLE_DISTANCE);
    }

    @Override
    protected Projectile createProjectile(int id, int ownerId, float x, float y, float dirX, float dirY) {
        return new Projectile(id, ownerId, x, y, PROJECTILE_RADIUS, dirX, dirY, PROJECTILE_SPEED, MAX_RANGE, DAMAGE);
    }
}
