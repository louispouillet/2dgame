package io.github.game2d.model.weapon;

import io.github.game2d.model.Projectile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntSupplier;

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

    protected Weapon(int maxAmmo, float cooldown, float reloadTime, AttackPattern pattern, float muzzleDistance) {
        this.maxAmmo = maxAmmo;
        this.cooldown = cooldown;
        this.reloadTime = reloadTime;
        this.pattern = pattern;
        this.muzzleDistance = muzzleDistance;
        this.ammo = maxAmmo;
    }

    public int getAmmo() {
        return ammo;
    }

    public int getMaxAmmo() {
        return maxAmmo;
    }

    public float getReloadProgress() {
        return reloadTimer / reloadTime;
    }

    /** True while bullets of the current attack still have to leave. */
    public boolean isAttacking() {
        return !pendingShots.isEmpty();
    }

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
