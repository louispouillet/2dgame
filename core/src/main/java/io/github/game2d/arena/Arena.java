package io.github.game2d.arena;

import java.util.List;
import java.util.function.Predicate;

public final class Arena {
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
