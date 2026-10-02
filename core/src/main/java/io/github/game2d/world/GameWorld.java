package io.github.game2d.world;
import io.github.game2d.collision.Collisions;
import io.github.game2d.model.GameObject;
import io.github.game2d.model.Projectile;
import io.github.game2d.brawler.Brawler;
import io.github.game2d.input.PlayerInput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameWorld {
    private final List<GameObject> gameObjects;

    public GameWorld() {
        this.gameObjects = new ArrayList<>();
    }

    public List<GameObject> getGameObjects() {
        return Collections.unmodifiableList(gameObjects);
    }

    public void add(GameObject object) {
        gameObjects.add(object);
    }

    public void update(float dt) {
        for (GameObject object : gameObjects) {
            object.update(dt);
        }
        resolveProjectileHits();
        gameObjects.removeIf(GameObject::isExpired);
    }

    private void resolveProjectileHits() {
        for (GameObject object : gameObjects) {
            if (!(object instanceof Projectile projectile) || projectile.isExpired()) {
                continue;
            }
            for (GameObject other : gameObjects) {
                if (other instanceof Brawler target
                        && target.isAlive()
                        && target.getId() != projectile.getOwnerId()
                        && Collisions.overlaps(projectile, target)) {
                    target.takeDamage(projectile.getDamage());
                    projectile.markHit();
                    break;
                }
            }
        }
    }

    public void attack(Brawler brawler, float aimX, float aimY) {
        gameObjects.addAll(brawler.attack(aimX, aimY));
    }

    public void applyInput(Brawler brawler, PlayerInput input) {
        brawler.setDirection(input.moveX(), input.moveY());
        if (input.shooting()) {
            attack(brawler, input.aimX(), input.aimY());
        }
    }
}
