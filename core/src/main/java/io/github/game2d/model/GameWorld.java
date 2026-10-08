package io.github.game2d.model;

import io.github.game2d.model.GameObject;
import io.github.game2d.model.Projectile;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.brawler.Brawler;
import io.github.game2d.model.collision.Collisions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GameWorld {
    private final List<GameObject> gameObjects;
    private final Map<Integer, Brawler> players;
    private final Arena arena;
    private int nextId = 1;

    public GameWorld(Arena arena) {
        this.gameObjects = new ArrayList<>();
        this.players = new HashMap<>();
        this.arena = Objects.requireNonNull(arena);
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

    public void removePlayer(int brawlerId) {
        Brawler brawler = players.remove(brawlerId);
        if (brawler != null) {
            gameObjects.remove(brawler);
        }
    }

    public Brawler getPlayer(int brawlerId) {
        return players.get(brawlerId);
    }

    public List<GameObject> getGameObjects() {
        return Collections.unmodifiableList(gameObjects);
    }

    public boolean isVisibleTo(Brawler target, Brawler viewer) {
        if (target == viewer || !arena.hidesBrawlerAt(target.getX(), target.getY())) {
            return true;
        }
        if (viewer == null) {
            return false;
        }
        float dx = target.getX() - viewer.getX();
        float dy = target.getY() - viewer.getY();
        return dx * dx + dy * dy <= Arena.REVEAL_DISTANCE * Arena.REVEAL_DISTANCE;
    }

    public void update(float dt) {
        for (GameObject object : gameObjects) {
            if (object instanceof Brawler brawler) {
                brawler.move(dt, arena);
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
                    && arena.blocksShots(projectile.getX(), projectile.getY(), projectile.getRadius())) {
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
                        && Collisions.sweptOverlaps(projectile, target)) {
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

}
