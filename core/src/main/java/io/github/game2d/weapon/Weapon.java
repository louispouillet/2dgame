package io.github.game2d.weapon;
import io.github.game2d.model.Projectile;
import java.util.List;

public abstract class Weapon {
    private final int maxAmmo;
    private final float cooldown;
    private final float reloadTime;

    private int ammo;
    private float cooldownTimer;
    private float reloadTimer;

    protected Weapon(int maxAmmo, float cooldown, float reloadTime) {
        this.maxAmmo = maxAmmo;
        this.cooldown = cooldown;
        this.reloadTime = reloadTime;
        this.ammo = maxAmmo;
    }

    public int getAmmo() {
        return ammo;
    }

    public int getMaxAmmo() {
        return maxAmmo;
    }

    protected abstract List<Projectile> createProjectiles(int ownerId, float x, float y, float dirX, float dirY);

    public List<Projectile> fire(int ownerId, float x, float y, float dirX, float dirY) {
        if (cooldownTimer > 0 || ammo == 0) {
            return List.of();
        }
        ammo--;
        cooldownTimer = cooldown;
        return createProjectiles(ownerId, x, y, dirX, dirY);
    }

    public void update(float dt) {
        cooldownTimer = Math.max(0, cooldownTimer - dt);
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
