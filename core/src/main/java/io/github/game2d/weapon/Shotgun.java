package io.github.game2d.weapon;
import java.util.List;
import java.util.ArrayList;
import io.github.game2d.model.Projectile;

public class Shotgun implements Weapon {
    private static final int NUM_PROJECTILES = 5;
    private static final float SPREAD_ANGLE = 10;
    private static final float PROJECTILE_SIZE = 0.1f;
    private static final float PROJECTILE_SPEED = 10f;

    @Override
    public List<Projectile> fire(float x, float y, float dirX, float dirY) {
        List<Projectile> projectiles = new ArrayList<>();
        for (int i = 0; i < NUM_PROJECTILES; i++) {
            float offset = i - (NUM_PROJECTILES - 1) / 2f;
            float angle = (float) Math.toRadians(offset * SPREAD_ANGLE);
            float newDirX = (float) (dirX * Math.cos(angle) - dirY * Math.sin(angle));
            float newDirY = (float) (dirX * Math.sin(angle) + dirY * Math.cos(angle));
            Projectile projectile = new Projectile(x, y, PROJECTILE_SIZE, PROJECTILE_SIZE, newDirX, newDirY, PROJECTILE_SPEED);
            projectiles.add(projectile);
        }
        return projectiles;
    }
}