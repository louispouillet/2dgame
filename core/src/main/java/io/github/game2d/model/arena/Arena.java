package io.github.game2d.model.arena;

import io.github.game2d.model.Team;
import io.github.game2d.model.collision.Collisions;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class Arena {
    public static final float REVEAL_DISTANCE = 2f;

    private final TileType[][] tiles;
    private final List<SpawnPoint> spawnPoints;

    Arena(TileType[][] tiles, List<SpawnPoint> spawnPoints) {
        this.tiles = new TileType[tiles.length][];
        for (int row = 0; row < tiles.length; row++) {
            this.tiles[row] = tiles[row].clone();
        }
        this.spawnPoints = List.copyOf(spawnPoints);
    }

    public int getWidth() {
        return tiles[0].length;
    }

    public int getHeight() {
        return tiles.length;
    }

    public TileType getTile(int col, int row) {
        if (col < 0 || row < 0 || col >= getWidth() || row >= getHeight()) {
            return TileType.WALL;
        }
        return tiles[row][col];
    }

    public boolean blocksMovement(float centerX, float centerY, float radius) {
        return anyTileTouchingCircle(centerX, centerY, radius, TileType::blocksMovement);
    }

    public boolean blocksShots(float centerX, float centerY, float radius) {
        return anyTileTouchingCircle(centerX, centerY, radius, TileType::blocksShots);
    }

    public boolean hidesBrawlerAt(float x, float y) {
        return getTile((int) Math.floor(x), (int) Math.floor(y)).hidesBrawler();
    }

    public boolean canSeeIntoBush(int col, int row, float viewerX, float viewerY) {
        if (!getTile(col, row).hidesBrawler()) {
            return false;
        }
        float dx = col + 0.5f - viewerX;
        float dy = row + 0.5f - viewerY;
        return dx * dx + dy * dy <= REVEAL_DISTANCE * REVEAL_DISTANCE;
    }

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

    public List<SpawnPoint> getSpawnPoints() {
        return spawnPoints;
    }

    public List<SpawnPoint> getSpawnPoints(Team team) {
        return spawnPoints.stream()
            .filter(spawn -> spawn.team() == team)
            .sorted(Comparator.comparingInt(SpawnPoint::player))
            .toList();
    }

    public SpawnPoint getSpawnPoint(int player) {
        for (SpawnPoint spawn : spawnPoints) {
            if (spawn.player() == player) {
                return spawn;
            }
        }
        throw new IllegalArgumentException("No spawn point for player " + player);
    }

}
