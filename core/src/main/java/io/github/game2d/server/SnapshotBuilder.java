package io.github.game2d.server;

import io.github.game2d.model.GameObject;
import io.github.game2d.model.GameWorld;
import io.github.game2d.model.Projectile;
import io.github.game2d.model.brawler.Brawler;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.MatchState;
import io.github.game2d.protocol.ProjectileState;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/** Turns the world into the snapshot one player is allowed to see (hidden enemies are left out). */
public class SnapshotBuilder {
    private final GameWorld world;

    /** Creates a builder for a world. */
    public SnapshotBuilder(GameWorld world) {
        this.world = world;
    }

    /**
     * Builds the snapshot of one player.
     *
     * @param tick          server tick number
     * @param lastProcessed gives the last input number applied for a brawler id
     * @param viewerId      id of the player who receives the snapshot
     * @param match         state of the match to copy in the snapshot
     */
    public WorldSnapshot build(int tick, IntUnaryOperator lastProcessed, int viewerId, MatchState match) {
        Brawler eyes = eyesOf(viewerId);
        List<BrawlerState> brawlerStates = new ArrayList<>();
        List<ProjectileState> projectileStates = new ArrayList<>();
        for (GameObject object : world.getGameObjects()) {
            if (object instanceof Projectile projectile) {
                Brawler owner = world.getPlayer(projectile.getOwnerId());
                projectileStates.add(
                    new ProjectileState(
                        projectile.getId(),
                        projectile.getOwnerId(),
                        owner == null ? null : owner.getType(),
                        projectile.getX(),
                        projectile.getY(),
                        projectile.getDirX(),
                        projectile.getDirY(),
                        projectile.getRadius()
                    )
                );
            }
            if (object instanceof Brawler brawler && world.isVisibleTo(brawler, eyes)) {
                int lastProcessedSequence = lastProcessed.applyAsInt(brawler.getId());
                brawlerStates.add(
                    new BrawlerState(
                        brawler.getId(),
                        brawler.getType(),
                        brawler.getX(),
                        brawler.getY(),
                        brawler.getRadius(),
                        brawler.getFacing(),
                        brawler.getHealth(),
                        brawler.getMaxHealth(),
                        brawler.getAmmo(),
                        brawler.getMaxAmmo(),
                        brawler.getReloadProgress(),
                        brawler.isMoving(),
                        brawler.isAttacking(),
                        brawler.getShotsFired(),
                        lastProcessedSequence
                    )
                );
            }
        }
        int followedId = eyes == null ? WorldSnapshot.NOBODY : eyes.getId();
        return new WorldSnapshot(tick, brawlerStates, projectileStates, match, followedId);
    }

    /**
     * Whose eyes a player sees through: their own while alive, then those of their living ally with the
     * smallest id, so that the watched ally does not change while it lives. Null when no ally is left.
     */
    private Brawler eyesOf(int viewerId) {
        Brawler viewer = world.getPlayer(viewerId);
        if (viewer != null && viewer.isAlive()) {
            return viewer;
        }
        Brawler eyes = null;
        for (GameObject object : world.getGameObjects()) {
            if (object instanceof Brawler ally && ally.isAlive() && world.areAllies(ally.getId(), viewerId)
                && (eyes == null || ally.getId() < eyes.getId())) {
                eyes = ally;
            }
        }
        return eyes;
    }
}
