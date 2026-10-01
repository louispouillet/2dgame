package io.github.game2d.brawler;
import io.github.game2d.model.GameObject;
import io.github.game2d.model.Projectile;
import io.github.game2d.weapon.Weapon;
import java.util.List;

public abstract class Brawler extends GameObject {
    private float dirX;
    private float dirY;
    private final float speed;
    private int maxHealth;
    private int health;
    private final Weapon weapon;

    protected Brawler(float x, float y, float width, float height, float speed, int maxHealth, Weapon weapon) {
        super(x, y, width, height);
        this.speed = speed;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.weapon = weapon;
    }

    public float getSpeed() {
        return speed;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getHealth() {
        return health;
    }
    
    public Weapon getWeapon() {
        return weapon;
    }

    @Override
    public void update(float dt) {
        if (!isAlive()) {
            return;
        }
        x += dirX * speed * dt;
        y += dirY * speed * dt;
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
        health -= damage;
        if (health <= 0) {
            health = 0;
        }
    }

    public boolean isAlive() {
        return health > 0;
    }

    public List<Projectile> attack(float aimX, float aimY) {
        if (!isAlive()) {
            return List.of();
        }
        return weapon.fire(x, y, aimX, aimY);
    }
}