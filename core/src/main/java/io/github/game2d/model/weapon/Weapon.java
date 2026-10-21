package io.github.game2d.model.weapon;

import io.github.game2d.model.Projectile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * What a {@link io.github.game2d.model.brawler.Brawler} shoots with: ammo, cooldown and reload.
 * The shape of the attack comes from an {@link AttackPattern}; subclasses only define the projectile
 * (Strategy pattern).
 */
public abstract class Weapon {
    private final int maxAmmo;
    private final float cooldown;
    private final float reloadTime;
    private final AttackPattern pattern;
    // Distance from the brawler's center to the gun muzzle, along the aim
    private final float muzzleDistance;

    private int ammo;
    private float cooldownTimer;
    private float reloadTimer;

    private final List<Shot> pendingShots = new ArrayList<>();
    private float attackTime;
    private float aimX;
    private float aimY;

    /**
     * Creates a full weapon.
     *
     * @param maxAmmo        ammo capacity
     * @param cooldown       seconds between two attacks
     * @param reloadTime     seconds to get one ammo back
     * @param pattern        the bullets fired by one attack
     * @param muzzleDistance distance between the brawler center and the bullet start, in tiles
     */
    protected Weapon(int maxAmmo, float cooldown, float reloadTime, AttackPattern pattern, float muzzleDistance) {
        this.maxAmmo = maxAmmo;
        this.cooldown = cooldown;
        this.reloadTime = reloadTime;
        this.pattern = pattern;
        this.muzzleDistance = muzzleDistance;
        this.ammo = maxAmmo;
    }

    /** Returns the ammo left. */
    public int getAmmo() {
        return ammo;
    }

    /** Returns the ammo capacity. */
    public int getMaxAmmo() {
        return maxAmmo;
    }

    /** Returns the progress of the current reload, from 0 to 1. */
    public float getReloadProgress() {
        return reloadTimer / reloadTime;
    }

    /** True while bullets of the current attack still have to leave. */
    public boolean isAttacking() {
        return !pendingShots.isEmpty();
    }

    /** Creates one bullet of this weapon, with its own size, speed, range and damage. */
    protected abstract Projectile createProjectile(int id, int ownerId, float x, float y, float dirX, float dirY);

    /** Uses one ammo and fires the immediate bullets of the attack; the others leave through {@link #releaseShots}. */
    public List<Projectile> fire(int ownerId, IntSupplier ids, float x, float y, float dirX, float dirY) {
        if (cooldownTimer > 0 || ammo == 0) {
            return List.of();
        }
        ammo--;
        cooldownTimer = cooldown;
        pendingShots.clear();
        pendingShots.addAll(pattern.shots());
        attackTime = 0;
        aimX = dirX;
        aimY = dirY;
        return releaseShots(ownerId, ids, x, y);
    }

    /** Fires, from the shooter's current position, the bullets of the current attack whose delay has passed. */
    public List<Projectile> releaseShots(int ownerId, IntSupplier ids, float x, float y) {
        List<Projectile> released = new ArrayList<>();
        Iterator<Shot> iterator = pendingShots.iterator();
        while (iterator.hasNext()) {
            Shot shot = iterator.next();
            if (shot.delay() > attackTime) {
                continue;
            }
            iterator.remove();
            // the bullet leaves from the muzzle: ahead of the shooter, then shifted sideways.
            // The right of the aim (aimX, aimY) is (aimY, -aimX)
            float startX = x + aimX * muzzleDistance + aimY * shot.sideOffset();
            float startY = y + aimY * muzzleDistance - aimX * shot.sideOffset();
            float angle = (float) Math.toRadians(shot.angle());
            float dirX = (float) (aimX * Math.cos(angle) - aimY * Math.sin(angle));
            float dirY = (float) (aimX * Math.sin(angle) + aimY * Math.cos(angle));
            released.add(createProjectile(ids.getAsInt(), ownerId, startX, startY, dirX, dirY));
        }
        return released;
    }

    /**
     * Advances the cooldown, the delayed bullets and the reload.
     *
     * @param dt elapsed time, in seconds
     */
    public void update(float dt) {
        cooldownTimer = Math.max(0, cooldownTimer - dt);
        if (!pendingShots.isEmpty()) {
            attackTime += dt;
        }
        if (ammo < maxAmmo) {
            reloadTimer += dt;
            if (reloadTimer >= reloadTime) {
                ammo++;
                reloadTimer -= reloadTime;
            }
        } else {
            reloadTimer = 0;
        }
    }
}
