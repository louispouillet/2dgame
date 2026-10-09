package io.github.game2d.model.arena;

import io.github.game2d.model.Team;

public record SpawnPoint(int player, float x, float y) {
    private static final int LAST_BLUE_PLAYER = 3;

    public Team team() {
        return player <= LAST_BLUE_PLAYER ? Team.BLUE : Team.RED;
    }
}
