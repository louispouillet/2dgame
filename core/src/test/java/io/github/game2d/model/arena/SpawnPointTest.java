package io.github.game2d.model.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.game2d.model.Team;
import org.junit.jupiter.api.Test;

class SpawnPointTest {
    @Test
    void spawnPointsOneToThreeAreBlue() {
        assertEquals(Team.BLUE, new SpawnPoint(1, 0f, 0f).team());
        assertEquals(Team.BLUE, new SpawnPoint(3, 0f, 0f).team());
    }

    @Test
    void spawnPointsFourToSixAreRed() {
        assertEquals(Team.RED, new SpawnPoint(4, 0f, 0f).team());
        assertEquals(Team.RED, new SpawnPoint(6, 0f, 0f).team());
    }
}
