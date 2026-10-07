package io.github.game2d.model.arena;

import static io.github.game2d.model.arena.TileType.BUSH;
import static io.github.game2d.model.arena.TileType.FLOOR;
import static io.github.game2d.model.arena.TileType.WALL;
import static io.github.game2d.model.arena.TileType.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArenaTest {
    private TileType[][] grid;
    private Arena arena;

    @BeforeEach
    void setUp() {
        grid = new TileType[][] {
            {WALL, WALL, WALL, WALL},
            {WALL, FLOOR, WATER, WALL},
            {WALL, BUSH, FLOOR, WALL},
            {WALL, WALL, WALL, WALL},
        };
        arena = new Arena(grid, List.of(new SpawnPoint(1, 1.5f, 1.5f), new SpawnPoint(2, 2.5f, 2.5f)));
    }

    @Test
    void sizeMatchesGrid() {
        assertEquals(4, arena.getWidth());
        assertEquals(4, arena.getHeight());
    }

    @Test
    void getTileReadsColumnThenRow() {
        assertEquals(WATER, arena.getTile(2, 1));
        assertEquals(BUSH, arena.getTile(1, 2));
    }

    @Test
    void getTileOutsideGridReturnsWall() {
        assertEquals(WALL, arena.getTile(-1, 1));
        assertEquals(WALL, arena.getTile(4, 1));
        assertEquals(WALL, arena.getTile(1, -1));
        assertEquals(WALL, arena.getTile(1, 4));
    }

    @Test
    void laterChangesToSourceGridDoNotAffectArena() {
        grid[1][1] = WALL;

        assertEquals(FLOOR, arena.getTile(1, 1));
    }

    @Test
    void getSpawnPointReturnsThePlayersSpawn() {
        assertEquals(new SpawnPoint(2, 2.5f, 2.5f), arena.getSpawnPoint(2));
    }

    @Test
    void getSpawnPointRejectsUnknownPlayer() {
        assertThrows(IllegalArgumentException.class, () -> arena.getSpawnPoint(3));
    }

    @Test
    void circleOnFloorIsNotBlocked() {
        assertFalse(arena.blocksMovement(1.5f, 1.5f, 0.25f));
        assertFalse(arena.blocksShots(1.5f, 1.5f, 0.25f));
    }

    @Test
    void circleOverlappingWallIsBlocked() {
        assertTrue(arena.blocksMovement(1.1f, 1.5f, 0.25f));
        assertTrue(arena.blocksShots(1.1f, 1.5f, 0.25f));
    }

    @Test
    void circleTouchingWallExactlyIsNotBlocked() {
        assertFalse(arena.blocksMovement(1.25f, 1.5f, 0.25f));
    }

    @Test
    void circleNearCornerIsNotBlocked() {
        assertFalse(arena.blocksMovement(1.8f, 2.2f, 0.25f));
    }

    @Test
    void circleOnWaterBlocksMovementButNotShots() {
        assertTrue(arena.blocksMovement(2.5f, 1.5f, 0.25f));
        assertFalse(arena.blocksShots(2.5f, 1.5f, 0.25f));
    }

    @Test
    void circleOnBushBlocksNothing() {
        assertFalse(arena.blocksMovement(1.5f, 2.5f, 0.25f));
        assertFalse(arena.blocksShots(1.5f, 2.5f, 0.25f));
    }

    @Test
    void circleOutsideArenaIsBlocked() {
        assertTrue(arena.blocksMovement(-1f, 1.5f, 0.25f));
    }

    @Test
    void bushHidesBrawler() {
        assertTrue(arena.hidesBrawlerAt(1.5f, 2.5f));
    }

    @Test
    void floorWaterAndWallDoNotHideBrawler() {
        assertFalse(arena.hidesBrawlerAt(1.5f, 1.5f));
        assertFalse(arena.hidesBrawlerAt(2.5f, 1.5f));
        assertFalse(arena.hidesBrawlerAt(0.5f, 0.5f));
    }

    @Test
    void onlyTheTileUnderTheCenterCounts() {
        assertFalse(arena.hidesBrawlerAt(2.1f, 2.5f));
    }

    @Test
    void outsideArenaDoesNotHideBrawler() {
        assertFalse(arena.hidesBrawlerAt(-1f, 2.5f));
    }

    @Test
    void spawnPointsCannotBeModified() {
        assertThrows(UnsupportedOperationException.class,
            () -> arena.getSpawnPoints().add(new SpawnPoint(3, 1.5f, 2.5f)));
    }
}
