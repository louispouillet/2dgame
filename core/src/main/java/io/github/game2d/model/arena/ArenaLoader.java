package io.github.game2d.model.arena;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Reads maps from text files. See {@link #parse} for the format. */
public final class ArenaLoader {
    /** Map used when nobody chooses another one (and in solo). */
    public static final String DEFAULT_MAP = "maps/Etoile-Filante.txt";
    private static final String ASSET_LIST = "assets.txt";
    private static final String MAP_FOLDER = "maps/";
    private static final String MAP_EXTENSION = ".txt";

    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 6;

    private ArenaLoader() {
    }

    /**
     * Loads and parses a map from the classpath.
     *
     * @param path path of the map, for example {@code maps/Etoile-Filante.txt}
     * @throws ArenaFormatException if the map is invalid
     */
    public static Arena loadResource(String path) {
        return parse(readResource(path));
    }

    /**
     * Lists the maps of {@code assets/maps/}, sorted, from the {@code assets.txt} file generated at build time.
     */
    public static List<String> availableMaps() {
        return readResource(ASSET_LIST).lines()
            .filter(path -> path.startsWith(MAP_FOLDER) && path.endsWith(MAP_EXTENSION))
            .sorted()
            .toList();
    }

    /** Reads a whole classpath resource as UTF-8 text. */
    private static String readResource(String path) {
        try (InputStream input = ArenaLoader.class.getResourceAsStream("/" + path)) {
            if (input == null) {
                throw new IllegalArgumentException("Resource not found: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + path, e);
        }
    }

    /**
     * Parses the text of a map.
     *
     * <p>The first line is the top of the map. Symbols: {@code #} wall, {@code .} floor, {@code ~} bush,
     * {@code W} water, {@code 1} to {@code 6} spawn points (on floor). The border must be a wall,
     * all lines must have the same length, each spawn number appears at most once and there are at least two.
     *
     * @throws ArenaFormatException if the text breaks one of these rules
     */
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

    /** Splits the text into lines and drops the blank lines at the end. */
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

    /** Whether a symbol is a spawn point digit (1 to 6). */
    private static boolean isSpawnSymbol(char symbol) {
        int digit = Character.digit(symbol, 10);
        return digit >= 1 && digit <= MAX_PLAYERS;
    }

    /** Converts a symbol to a tile. Spawn points are floor. */
    private static TileType readTile(char symbol, int line, int col) {
        if (isSpawnSymbol(symbol)) {
            return TileType.FLOOR;
        }
        return TileType.fromSymbol(symbol)
            .orElseThrow(() -> errorAt(line, col, "unknown symbol '" + symbol + "'"));
    }

    /**
     * Registers a spawn point, centered on its tile.
     *
     * @throws ArenaFormatException if the number is already used
     */
    private static void addSpawnPoint(List<SpawnPoint> spawnPoints, int player, int line, int col, int row) {
        for (SpawnPoint spawn : spawnPoints) {
            if (spawn.player() == player) {
                throw errorAt(line, col, "spawn point " + player + " is defined twice");
            }
        }
        spawnPoints.add(new SpawnPoint(player, col + 0.5f, row + 0.5f));
    }

    /** Builds an exception whose message gives the position (1-based) of the problem. */
    private static ArenaFormatException errorAt(int line, int col, String message) {
        return new ArenaFormatException("Line " + (line + 1) + ", column " + (col + 1) + ": " + message);
    }
}
