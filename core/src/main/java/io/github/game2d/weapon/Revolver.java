package io.github.game2d.weapon;
import java.util.List;
import io.github.game2d.model.Projectile;

public class Revolver implements Weapon {
    private static final float PROJECTILE_SIZE = 0.1f;
    private static final float PROJECTILE_SPEED = 12f;

    @Override
    public List<Projectile> fire(float x, float y, float dirX, float dirY) {
        Projectile projectile = new Projectile(x, y, PROJECTILE_SIZE, PROJECTILE_SIZE, dirX, dirY, PROJECTILE_SPEED);
        return List.of(projectile);
    }
}