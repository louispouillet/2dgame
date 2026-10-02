package io.github.game2d.weapon;
import java.util.List;
import java.util.ArrayList;
import io.github.game2d.model.Projectile;

public class Shotgun extends Weapon {
    private static final int MAX_AMMO = 3;
    private static final float COOLDOWN = 0.6f;
    private static final float RELOAD_TIME = 1.5f;
    private static final float MAX_RANGE = 4f;
    private static final int NUM_PROJECTILES = 5;
    private static final float SPREAD_ANGLE = 10;
    private static final float PROJECTILE_RADIUS = 0.05f;
    private static final float PROJECTILE_SPEED = 10f;
    private static final int DAMAGE = 15;

    public Shotgun() {
        super(MAX_AMMO, COOLDOWN, RELOAD_TIME);
    }

    @Override
    protected List<Projectile> createProjectiles(int ownerId, float x, float y, float dirX, float dirY) {
        List<Projectile> projectiles = new ArrayList<>();
        for (int i = 0; i < NUM_PROJECTILES; i++) {
            float offset = i - (NUM_PROJECTILES - 1) / 2f;
            float angle = (float) Math.toRadians(offset * SPREAD_ANGLE);
            float newDirX = (float) (dirX * Math.cos(angle) - dirY * Math.sin(angle));
            float newDirY = (float) (dirX * Math.sin(angle) + dirY * Math.cos(angle));
            Projectile projectile = new Projectile(ownerId, x, y, PROJECTILE_RADIUS, newDirX, newDirY, PROJECTILE_SPEED, MAX_RANGE, DAMAGE);
            projectiles.add(projectile);
        }
        return projectiles;
    }
}