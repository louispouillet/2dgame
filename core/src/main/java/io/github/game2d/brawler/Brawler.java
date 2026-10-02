package io.github.game2d.brawler;
import io.github.game2d.collision.Circle;
import io.github.game2d.model.GameObject;
import io.github.game2d.model.Projectile;
import io.github.game2d.weapon.Weapon;
import java.util.List;

public abstract class Brawler extends GameObject {
    private static final float REGEN_DELAY = 3f;
    private static final float REGEN_RATE = 0.2f;

    private final Circle hitbox;
    private final float speed;
    private final int maxHealth;
    private final Weapon weapon;

    private float dirX;
    private float dirY;
    private int health;
    private float timeSinceCombat;
    private float regenBuffer;

    protected Brawler(float x, float y, Circle hitbox, float speed, int maxHealth, Weapon weapon) {
        super(x, y);
        this.hitbox = hitbox;
        this.speed = speed;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.weapon = weapon;
    }

    @Override
    public Circle getHitbox() {
        return hitbox;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getAmmo() {
        return weapon.getAmmo();
    }

    public int getMaxAmmo() {
        return weapon.getMaxAmmo();
    }

    public boolean isAlive() {
        return health > 0;
    }

    @Override
    public boolean isExpired() {
        return !isAlive();
    }

    @Override
    public void update(float dt) {
        if (!isAlive()) {
            return;
        }
        x += dirX * speed * dt;
        y += dirY * speed * dt;
        weapon.update(dt);
        regenerate(dt);
    }

    private void regenerate(float dt) {
        timeSinceCombat += dt;
        if (timeSinceCombat < REGEN_DELAY || health >= maxHealth) {
            regenBuffer = 0;
            return;
        }
        regenBuffer += maxHealth * REGEN_RATE * dt;
        int gained = (int) regenBuffer;
        health = Math.min(maxHealth, health + gained);
        regenBuffer -= gained;
    }

    public void setDirection(float dirX, float dirY) {
        float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (length == 0) {
            this.dirX = 0;
            this.dirY = 0;
        } else {
            this.dirX = dirX / length;
            this.dirY = dirY / length;
        }
    }

    public void takeDamage(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("Damage cannot be negative");
        }
        health = Math.max(0, health - damage);
        timeSinceCombat = 0;
    }

    public List<Projectile> attack(float aimX, float aimY) {
        if (!isAlive()) {
            return List.of();
        }
        List<Projectile> projectiles = weapon.fire(getId(), x, y, aimX, aimY);
        if (!projectiles.isEmpty()) {
            timeSinceCombat = 0;
        }
        return projectiles;
    }

    public float getReloadProgress() {
        return weapon.getReloadProgress();
    }
}
