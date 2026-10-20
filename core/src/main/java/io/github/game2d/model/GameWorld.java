package io.github.game2d.model;

import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.brawler.Brawler;
import io.github.game2d.model.collision.Collisions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The real state of a match: the brawlers, their teams and the projectiles, inside an {@link Arena}.
 * Only the server owns one; clients draw a copy built from snapshots.
 */
public class GameWorld {
    private final List<GameObject> gameObjects;
    private final Map<Integer, Brawler> players;
    private final Map<Integer, Team> teams;
    private final Arena arena;
    private int nextId = 1;

    /**
     * Creates an empty world.
     *
     * @param arena the scenery of the match
     */
    public GameWorld(Arena arena) {
        this.gameObjects = new ArrayList<>();
        this.players = new HashMap<>();
        this.teams = new HashMap<>();
        this.arena = Objects.requireNonNull(arena);
    }

    /** Returns a new id, unique in this world. */
    public int nextId() {
        return nextId++;
    }

    /**
     * Adds a brawler to the world and registers its team.
     *
     * @throws IllegalArgumentException if a brawler with the same id is already registered
     */
    public void addPlayer(Brawler brawler, Team team) {
        if (players.containsKey(brawler.getId())) {
            throw new IllegalArgumentException("Brawler already registered: " + brawler.getId());
        }
        players.put(brawler.getId(), brawler);
        teams.put(brawler.getId(), Objects.requireNonNull(team));
        nextId = Math.max(nextId, brawler.getId() + 1);
        gameObjects.add(brawler);
    }

    /** Removes every object and every player. Ids keep growing, so they stay unique across rounds. */
    public void clear() {
        gameObjects.clear();
        players.clear();
        teams.clear();
    }

    /** Removes a brawler and forgets its team. Does nothing for an unknown id. */
    public void removePlayer(int brawlerId) {
        Brawler brawler = players.remove(brawlerId);
        teams.remove(brawlerId);
        if (brawler != null) {
            gameObjects.remove(brawler);
        }
    }

    /** Returns the brawler with this id, or {@code null} if there is none. */
    public Brawler getPlayer(int brawlerId) {
        return players.get(brawlerId);
    }

    /** Returns how many brawlers of a team are still alive. */
    public int aliveCount(Team team) {
        int alive = 0;
        for (Brawler player : players.values()) {
            if (teams.get(player.getId()) == team && player.isAlive()) {
                alive++;
            }
        }
        return alive;
    }

    /**
     * Whether two players are in the same team. A player is its own ally; an unknown player has no ally.
     */
    public boolean areAllies(int firstId, int secondId) {
        Team first = teams.get(firstId);
        return first != null && first == teams.get(secondId);
    }

    /** Returns a read-only view of every object (brawlers and projectiles). */
    public List<GameObject> getGameObjects() {
        return Collections.unmodifiableList(gameObjects);
    }

    /**
     * Whether a viewer is allowed to see a brawler.
     * A brawler is hidden only while standing in a bush, and not exposed by an attack. Then only the viewer itself,
     * its allies and enemies closer than {@link Arena#REVEAL_DISTANCE} see it.
     *
     * @param target the brawler that may be hidden
     * @param viewer the brawler whose eyes are used, or {@code null} for nobody
     */
    public boolean isVisibleTo(Brawler target, Brawler viewer) {
        if (!arena.hidesBrawlerAt(target.getX(), target.getY())) {
            return true;
        }
        if (target.isExposed()) {
            return true;
        }
        if (viewer == null) {
            return false;
        }
        if (target == viewer || areAllies(target.getId(), viewer.getId())) {
            return true;
        }
        float dx = target.getX() - viewer.getX();
        float dy = target.getY() - viewer.getY();
        return dx * dx + dy * dy <= Arena.REVEAL_DISTANCE * Arena.REVEAL_DISTANCE;
    }

    /**
     * Advances the whole world by one tick: updates every object, releases the pending shots,
     * resolves the projectile hits, then removes what has expired.
     *
     * @param dt elapsed time, in seconds
     */
    public void update(float dt) {
        for (GameObject object : gameObjects) {
            object.update(dt, arena);
        }
        for (Brawler player : players.values()) {
            gameObjects.addAll(player.releaseShots(this::nextId));
        }
        resolveProjectileHits();
        gameObjects.removeIf(GameObject::isExpired);
    }

    /**
     * Applies the damage of each projectile to the first enemy brawler it touched along its path. Allies are ignored.
     */
    private void resolveProjectileHits() {
        for (GameObject object : gameObjects) {
            if (!(object instanceof Projectile projectile) || projectile.isExpired()) {
                continue;
            }
            for (GameObject other : gameObjects) {
                if (other instanceof Brawler target
                        && target.isAlive()
                        && !areAllies(target.getId(), projectile.getOwnerId())
                        && Collisions.sweptOverlaps(projectile, target)) {
                    target.takeDamage(projectile.getDamage());
                    projectile.markHit();
                    break;
                }
            }
        }
    }

    /**
     * Makes a brawler attack towards a direction and adds the projectiles that leave immediately.
     *
     * @param aimX horizontal component of the aim direction
     * @param aimY vertical component of the aim direction
     */
    public void attack(Brawler brawler, float aimX, float aimY) {
        gameObjects.addAll(brawler.attack(this::nextId, aimX, aimY));
    }

}
