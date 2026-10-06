package io.github.game2d.model.arena;

import io.github.game2d.model.collision.Obstacles;
import java.util.List;
import java.util.function.Predicate;

public final class Arena implements Obstacles {
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

    public boolean blocksMovement(float x, float y, float width, float height) {
        return anyTile(x, y, width, height, TileType::blocksMovement);
    }

    public boolean blocksShots(float x, float y, float width, float height) {
        return anyTile(x, y, width, height, TileType::blocksShots);
    }

    @Override
    public boolean blocksMovement(float centerX, float centerY, float radius) {
        return anyTileTouchingCircle(centerX, centerY, radius, TileType::blocksMovement);
    }

    @Override
    public boolean blocksShots(float centerX, float centerY, float radius) {
        return anyTileTouchingCircle(centerX, centerY, radius, TileType::blocksShots);
    }

    private boolean anyTileTouchingCircle(float centerX, float centerY, float radius, Predicate<TileType> condition) {
        int firstCol = (int) Math.floor(centerX - radius);
        int lastCol = (int) Math.floor(centerX + radius);
        int firstRow = (int) Math.floor(centerY - radius);
        int lastRow = (int) Math.floor(centerY + radius);
        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = firstCol; col <= lastCol; col++) {
                if (!condition.test(getTile(col, row))) {
                    continue;
                }
                float nearestX = Math.clamp(centerX, col, col + 1);
                float nearestY = Math.clamp(centerY, row, row + 1);
                float dx = centerX - nearestX;
                float dy = centerY - nearestY;
                if (dx * dx + dy * dy < radius * radius) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<SpawnPoint> getSpawnPoints() {
        return spawnPoints;
    }

    public SpawnPoint getSpawnPoint(int player) {
        for (SpawnPoint spawn : spawnPoints) {
            if (spawn.player() == player) {
                return spawn;
            }
        }
        throw new IllegalArgumentException("No spawn point for player " + player);
    }

    private boolean anyTile(float x, float y, float width, float height, Predicate<TileType> condition) {
        int firstCol = (int) Math.floor(x);
        int lastCol = (int) Math.ceil(x + width) - 1;
        int firstRow = (int) Math.floor(y);
        int lastRow = (int) Math.ceil(y + height) - 1;
        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = firstCol; col <= lastCol; col++) {
                if (condition.test(getTile(col, row))) {
                    return true;
                }
            }
        }
        return false;
    }
}
