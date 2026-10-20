package io.github.game2d.protocol;

import java.util.List;

/**
 * The world as one player is allowed to see it at one tick. Enemies hidden in bushes are left out.
 *
 * @param tick        number of the server tick
 * @param brawlers    the visible brawlers
 * @param projectiles every projectile
 * @param match       where the match stands
 * @param followedId  id of the brawler whose view this is: the player while alive, then a living ally,
 *                    or {@link #NOBODY}
 */
public record WorldSnapshot(
    int tick,
    List<BrawlerState> brawlers,
    List<ProjectileState> projectiles,
    MatchState match,
    // The brawler whose view this snapshot shows: the player while alive, then a living ally, or NOBODY
    int followedId
) {
    /** Value of {@code followedId} when the whole team is dead. */
    public static final int NOBODY = -1;

    /** Returns the state of a brawler, or {@code null} if it is absent (dead or hidden). */
    public BrawlerState findBrawler(int id) {
        for (BrawlerState brawler : brawlers) {
            if (brawler.id() == id) {
                return brawler;
            }
        }
        return null;
    }
}
