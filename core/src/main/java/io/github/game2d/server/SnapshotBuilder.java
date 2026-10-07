package io.github.game2d.server;

import io.github.game2d.model.GameObject;
import io.github.game2d.model.GameWorld;
import io.github.game2d.model.Projectile;
import io.github.game2d.model.brawler.Brawler;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.ProjectileState;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.ArrayList;
import java.util.List;

public class SnapshotBuilder {
    private final GameWorld world;

    public SnapshotBuilder(GameWorld world) {
        this.world = world;
    }

    public WorldSnapshot build() {
        List<BrawlerState> brawlerStates = new ArrayList<>();
        List<ProjectileState> projectileStates = new ArrayList<>();
        for (GameObject object : world.getGameObjects()) {
            if (object instanceof Projectile projectile) {
                projectileStates.add(
                    new ProjectileState(
                        projectile.getId(),
                        projectile.getX(),
                        projectile.getY(),
                        projectile.getRadius()
                    )
                );
            }
            if (object instanceof Brawler brawler) {
                brawlerStates.add(
                    new BrawlerState(
                        brawler.getId(),
                        brawler.getX(),
                        brawler.getY(),
                        brawler.getRadius(),
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
