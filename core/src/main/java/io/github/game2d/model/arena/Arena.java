package io.github.game2d.model.arena;

import io.github.game2d.model.Team;
import io.github.game2d.model.collision.Collisions;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * The scenery: a grid of tiles and the spawn points. Immutable.
 * Tile {@code (col, row)} covers the square from {@code (col, row)} to {@code (col + 1, row + 1)}; anything outside the grid counts as a wall.
 */
public final class Arena {
    /** Distance (in tiles) under which an enemy sees a brawler hidden in a bush. */
    public static final float REVEAL_DISTANCE = 2f;

    private final TileType[][] tiles;
    private final List<SpawnPoint> spawnPoints;

    /** Creates an arena; use {@link ArenaLoader} to build one. The arrays are copied. */
    Arena(TileType[][] tiles, List<SpawnPoint> spawnPoints) {
        this.tiles = new TileType[tiles.length][];
        for (int row = 0; row < tiles.length; row++) {
            this.tiles[row] = tiles[row].clone();
        }
        this.spawnPoints = List.copyOf(spawnPoints);
    }

    /** Returns the number of columns. */
    public int getWidth() {
        return tiles[0].length;
    }

    /** Returns the number of rows. */
    public int getHeight() {
        return tiles.length;
    }

    /** Returns the tile at a position, or {@link TileType#WALL} outside the grid. */
    public TileType getTile(int col, int row) {
        if (col < 0 || row < 0 || col >= getWidth() || row >= getHeight()) {
            return TileType.WALL;
        }
        return tiles[row][col];
    }

    /** Whether a circle would touch a tile that blocks walking (wall or water). */
    public boolean blocksMovement(float centerX, float centerY, float radius) {
        return anyTileTouchingCircle(centerX, centerY, radius, TileType::blocksMovement);
    }

    /** Whether a circle would touch a tile that stops bullets (wall). */
    public boolean blocksShots(float centerX, float centerY, float radius) {
        return anyTileTouchingCircle(centerX, centerY, radius, TileType::blocksShots);
    }

    /** Whether the tile under a point hides the brawler standing there (bush). */
    public boolean hidesBrawlerAt(float x, float y) {
        return getTile((int) Math.floor(x), (int) Math.floor(y)).hidesBrawler();
    }

    /**
     * Whether a bush tile is close enough to a viewer to be drawn transparent.
     *
     * @param col     column of the tile
     * @param row     row of the tile
     * @param viewerX horizontal position of the viewer
     * @param viewerY vertical position of the viewer
     */
    public boolean canSeeIntoBush(int col, int row, float viewerX, float viewerY) {
        if (!getTile(col, row).hidesBrawler()) {
            return false;
        }
        float dx = col + 0.5f - viewerX;
        float dy = row + 0.5f - viewerY;
        return dx * dx + dy * dy <= REVEAL_DISTANCE * REVEAL_DISTANCE;
    }

    /** Whether a circle touches at least one tile that satisfies a condition. */
    private boolean anyTileTouchingCircle(float centerX, float centerY, float radius, Predicate<TileType> condition) {
        int firstCol = (int) Math.floor(centerX - radius);
        int lastCol = (int) Math.floor(centerX + radius);
        int firstRow = (int) Math.floor(centerY - radius);
        int lastRow = (int) Math.floor(centerY + radius);
        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = firstCol; col <= lastCol; col++) {
                if (condition.test(getTile(col, row))
                        && Collisions.circleTouchesBox(centerX, centerY, radius, col, row, col + 1, row + 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Returns every spawn point of the map. */
    public List<SpawnPoint> getSpawnPoints() {
        return spawnPoints;
    }

    /** Returns the spawn points of a team, sorted by number. */
    public List<SpawnPoint> getSpawnPoints(Team team) {
        return spawnPoints.stream()
            .filter(spawn -> spawn.team() == team)
            .sorted(Comparator.comparingInt(SpawnPoint::player))
            .toList();
    }

    /**
     * Returns the spawn point with this number.
     *
     * @throws IllegalArgumentException if the map has no such point
     */
    public SpawnPoint getSpawnPoint(int player) {
        for (SpawnPoint spawn : spawnPoints) {
            if (spawn.player() == player) {
                return spawn;
            }
        }
        throw new IllegalArgumentException("No spawn point for player " + player);
    }

}
