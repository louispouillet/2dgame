package io.github.game2d.model.weapon;

import io.github.game2d.model.Projectile;

/** Long range weapon: each attack is a burst of fast, thin bullets. */
public class Revolver extends Weapon {
    private static final int MAX_AMMO = 3;
    private static final float COOLDOWN = 1f;
    private static final float RELOAD_TIME = 1.5f;
    private static final float MAX_RANGE = 8f;
    private static final float PROJECTILE_RADIUS = 0.1f;
    private static final float PROJECTILE_SPEED = 12f;
    private static final int DAMAGE = 720;
    private static final int BULLETS_PER_BURST = 6;
    private static final float BULLET_INTERVAL = 0.1f;
    private static final float SIDE_OFFSET = 0.25f;
    private static final float MUZZLE_DISTANCE = 1f;

    /** Creates a full revolver. */
    public Revolver() {
        super(MAX_AMMO, COOLDOWN, RELOAD_TIME, new BurstPattern(BULLETS_PER_BURST, BULLET_INTERVAL, SIDE_OFFSET), MUZZLE_DISTANCE);
    }

    @Override
    protected Projectile createProjectile(int id, int ownerId, float x, float y, float dirX, float dirY) {
        return new Projectile(id, ownerId, x, y, PROJECTILE_RADIUS, dirX, dirY, PROJECTILE_SPEED, MAX_RANGE, DAMAGE);
    }
}
