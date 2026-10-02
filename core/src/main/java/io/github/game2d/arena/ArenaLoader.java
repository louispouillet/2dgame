package io.github.game2d.arena;

import java.util.ArrayList;
import java.util.List;

public final class ArenaLoader {
    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 4;

    private ArenaLoader() {
    }

    public static Arena parse(String text) {
        List<String> lines = readLines(text);
        int height = lines.size();
        int width = lines.get(0).length();
        TileType[][] tiles = new TileType[height][width];
        List<SpawnPoint> spawnPoints = new ArrayList<>();

        for (int line = 0; line < height; line++) {
            String content = lines.get(line);
            if (content.length() != width) {
                throw new ArenaFormatException("Line " + (line + 1) + " has " + content.length()
                    + " tiles, expected " + width);
            }
            int row = height - 1 - line;
            for (int col = 0; col < width; col++) {
                char symbol = content.charAt(col);
                if (isSpawnSymbol(symbol)) {
                    addSpawnPoint(spawnPoints, Character.digit(symbol, 10), line, col, row);
                }
                TileType tile = readTile(symbol, line, col);
                boolean onBorder = line == 0 || line == height - 1 || col == 0 || col == width - 1;
                if (onBorder && tile != TileType.WALL) {
                    throw errorAt(line, col, "the border must be a wall");
                }
                tiles[row][col] = tile;
            }
        }

        if (spawnPoints.size() < MIN_PLAYERS) {
            throw new ArenaFormatException("The map needs at least " + MIN_PLAYERS
                + " spawn points, found " + spawnPoints.size());
        }
        return new Arena(tiles, spawnPoints);
    }

    private static List<String> readLines(String text) {
        if (text == null || text.isBlank()) {
            throw new ArenaFormatException("The map is empty");
        }
        List<String> lines = new ArrayList<>(text.lines().toList());
        while (lines.get(lines.size() - 1).isBlank()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    private static boolean isSpawnSymbol(char symbol) {
        int digit = Character.digit(symbol, 10);
        return digit >= 1 && digit <= MAX_PLAYERS;
    }

    private static TileType readTile(char symbol, int line, int col) {
        if (isSpawnSymbol(symbol)) {
            return TileType.FLOOR;
        }
        return TileType.fromSymbol(symbol)
            .orElseThrow(() -> errorAt(line, col, "unknown symbol '" + symbol + "'"));
    }

    private static void addSpawnPoint(List<SpawnPoint> spawnPoints, int player, int line, int col, int row) {
        for (SpawnPoint spawn : spawnPoints) {
            if (spawn.player() == player) {
                throw errorAt(line, col, "spawn point " + player + " is defined twice");
            }
        }
        spawnPoints.add(new SpawnPoint(player, col + 0.5f, row + 0.5f));
    }

    private static ArenaFormatException errorAt(int line, int col, String message) {
        return new ArenaFormatException("Line " + (line + 1) + ", column " + (col + 1) + ": " + message);
    }
}
