package io.github.game2d.model.arena;

import io.github.game2d.model.Team;

/**
 * A place where a player starts a round.
 *
 * @param player number from 1 to 6 (1 to 3 for the blue team, 4 to 6 for the red team)
 * @param x      horizontal position of the center of the tile
 * @param y      vertical position of the center of the tile
 */
public record SpawnPoint(int player, float x, float y) {
    private static final int LAST_BLUE_PLAYER = 3;

    /** Returns the team that owns this point, deduced from its number. */
    public Team team() {
        return player <= LAST_BLUE_PLAYER ? Team.BLUE : Team.RED;
    }
}
