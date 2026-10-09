package io.github.game2d.protocol;

import java.util.List;

public record WorldSnapshot(
    int tick,
    List<BrawlerState> brawlers,
    List<ProjectileState> projectiles
) {
    public BrawlerState findBrawler(int id) {
        for (BrawlerState brawler : brawlers) {
            if (brawler.id() == id) {
                return brawler;
            }
        }
        return null;
    }
}
