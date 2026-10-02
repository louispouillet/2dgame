package io.github.game2d.weapon;
import java.util.List;
import io.github.game2d.model.Projectile;

public class Revolver extends Weapon {
    private static final int MAX_AMMO = 3;
    private static final float COOLDOWN = 0.3f;
    private static final float RELOAD_TIME = 1.5f;
    private static final float MAX_RANGE = 8f;
    private static final float PROJECTILE_SIZE = 0.1f;
    private static final float PROJECTILE_SPEED = 12f;

    public Revolver() {
        super(MAX_AMMO, COOLDOWN, RELOAD_TIME);
    }

    @Override
    protected List<Projectile> createProjectiles(float x, float y, float dirX, float dirY) {
        Projectile projectile = new Projectile(x, y, PROJECTILE_SIZE, PROJECTILE_SIZE, dirX, dirY, PROJECTILE_SPEED, MAX_RANGE);
        return List.of(projectile);
    }
}
