package io.github.game2d.model;

import io.github.game2d.model.GameObject;
import io.github.game2d.model.Projectile;
import io.github.game2d.model.brawler.Brawler;
import io.github.game2d.model.collision.Collisions;
import io.github.game2d.model.collision.Obstacles;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.ProjectileState;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GameWorld {
    private final List<GameObject> gameObjects;
    private final Map<Integer, Brawler> players;
    private final Obstacles obstacles;
    private int nextId = 1;

    public GameWorld(Obstacles obstacles) {
        this.gameObjects = new ArrayList<>();
        this.players = new HashMap<>();
        this.obstacles = Objects.requireNonNull(obstacles);
    }

    public int nextId() {
        return nextId++;
    }

    public void addPlayer(Brawler brawler) {
        if (players.containsKey(brawler.getId())) {
            throw new IllegalArgumentException("Brawler already registered: " + brawler.getId());
        }
        players.put(brawler.getId(), brawler);
        gameObjects.add(brawler);
    }

    public Brawler getPlayer(int brawlerId) {
        return players.get(brawlerId);
    }

    public List<GameObject> getGameObjects() {
        return Collections.unmodifiableList(gameObjects);
    }

    public void update(float dt) {
        for (GameObject object : gameObjects) {
            if (object instanceof Brawler brawler) {
                brawler.move(dt, obstacles);
            }
            object.update(dt);
        }
        stopProjectilesOnObstacles();
        resolveProjectileHits();
        gameObjects.removeIf(GameObject::isExpired);
    }

    private void stopProjectilesOnObstacles() {
        for (GameObject object : gameObjects) {
            if (object instanceof Projectile projectile
                    && obstacles.blocksShots(projectile.getX(), projectile.getY(), projectile.getHitbox().radius())) {
                projectile.markHit();
            }
        }
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
        gameObjects.addAll(brawler.attack(this::nextId, aimX, aimY));
    }

    public void applyInput(int brawlerId, PlayerInput input) {
        Brawler brawler = players.get(brawlerId);
        if (brawler != null) {
            brawler.setDirection(input.moveX(), input.moveY());
            if (input.shooting()) {
                attack(brawler, input.aimX(), input.aimY());
            }
        }
    }

    public WorldSnapshot snapshot() {
        List<BrawlerState> brawlerStates = new ArrayList<>();
        List<ProjectileState> projectileStates = new ArrayList<>();
        for (GameObject object : gameObjects) {
            if (object instanceof Projectile projectile) {
                projectileStates.add(
                    new ProjectileState(
                        projectile.getId(),
                        projectile.getX(),
                        projectile.getY(),
                        projectile.getHitbox().radius()
                    )
                );
            }
            if (object instanceof Brawler brawler) {
                brawlerStates.add(
                    new BrawlerState(
                        brawler.getId(),
                        brawler.getX(),
                        brawler.getY(),
                        brawler.getHitbox().radius(),
                        brawler.getHealth(),
                        brawler.getMaxHealth(),
                        brawler.getAmmo(),
                        brawler.getMaxAmmo(),
                        brawler.getReloadProgress()
                    )
                );
            }
        }
        return new WorldSnapshot(brawlerStates, projectileStates);
    }
}
