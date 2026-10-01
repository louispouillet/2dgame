package io.github.game2d.weapon;
import java.util.List;
import io.github.game2d.model.Projectile;


public interface Weapon {

    public List<Projectile> fire(float x, float y, float dirX, float dirY);
}