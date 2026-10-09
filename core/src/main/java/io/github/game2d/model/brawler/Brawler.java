package io.github.game2d.model.brawler;

import io.github.game2d.model.GameObject;
import io.github.game2d.model.Projectile;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.weapon.Weapon;
import java.util.List;
import java.util.function.IntSupplier;

public abstract class Brawler extends GameObject {
    private static final float REGEN_DELAY = 3f;
    private static final float REGEN_RATE = 0.2f;
    private static final int MOVE_ATTEMPTS = 5;

    private float dirX;
    private float dirY;
    private final float speed;
    private final int maxHealth;
    private int health;
    private float timeSinceCombat;
    private float regenBuffer;
    private final Weapon weapon;

    protected Brawler(int id, float x, float y, float radius, float speed, int maxHealth, Weapon weapon) {
        super(id, x, y, radius);
        this.speed = speed;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.weapon = weapon;
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

    public abstract BrawlerType getType();

    @Override
    public boolean isExpired() {
        return !isAlive();
    }

    @Override
    public void update(float dt, Arena arena) {
        if (!isAlive()) {
            return;
        }
        move(dt, arena);
        weapon.update(dt);
        regenerate(dt);
    }

    private void move(float dt, Arena arena) {
        float stepX = dirX * speed * dt;
        for (int i = 0; i < MOVE_ATTEMPTS && stepX != 0; i++, stepX /= 2) {
            if (!arena.blocksMovement(x + stepX, y, radius)) {
                x += stepX;
                break;
            }
        }
        float stepY = dirY * speed * dt;
        for (int i = 0; i < MOVE_ATTEMPTS && stepY != 0; i++, stepY /= 2) {
            if (!arena.blocksMovement(x, y + stepY, radius)) {
                y += stepY;
                break;
            }
        }
    }

    public void correctPositionTo(float x, float y, Arena arena){
        if (arena.blocksMovement(x, y, radius)) {
            throw new IllegalArgumentException("Position blocked by the arena");
        }
        this.x = x;
        this.y = y;
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

    public List<Projectile> attack(IntSupplier ids, float aimX, float aimY) {
        if (!isAlive()) {
            return List.of();
        }
        List<Projectile> projectiles = weapon.fire(getId(), ids, x, y, aimX, aimY);
        if (!projectiles.isEmpty()) {
            timeSinceCombat = 0;
        }
        return projectiles;
    }

    public float getReloadProgress() {
        return weapon.getReloadProgress();
    }
}
