package io.github.game2d.protocol;

import java.util.List;

public record WorldSnapshot(
    int tick,
    List<BrawlerState> brawlers,
    List<ProjectileState> projectiles,
    MatchState match,
    // The brawler whose view this snapshot shows: the player while alive, then a living ally, or NOBODY
    int followedId
) {
    public static final int NOBODY = -1;

    public BrawlerState findBrawler(int id) {
        for (BrawlerState brawler : brawlers) {
            if (brawler.id() == id) {
                return brawler;
            }
        }
        return null;
    }
}
