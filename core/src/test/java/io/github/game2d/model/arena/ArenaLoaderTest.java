package io.github.game2d.model.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ArenaLoaderTest {
    private static final String VALID_MAP = """
        ######
        #1~.2#
        #.WW.#
        ######
        """;

    @Test
    void parsesSize() {
        Arena arena = ArenaLoader.parse(VALID_MAP);

        assertEquals(6, arena.getWidth());
        assertEquals(4, arena.getHeight());
    }

    @Test
    void firstLineOfFileIsTopRow() {
        Arena arena = ArenaLoader.parse(VALID_MAP);

        assertEquals(TileType.BUSH, arena.getTile(2, 2));
        assertEquals(TileType.WATER, arena.getTile(2, 1));
    }

    @Test
    void spawnPointIsCenteredOnAFloorTile() {
        Arena arena = ArenaLoader.parse(VALID_MAP);

        assertEquals(new SpawnPoint(1, 1.5f, 2.5f), arena.getSpawnPoint(1));
        assertEquals(new SpawnPoint(2, 4.5f, 2.5f), arena.getSpawnPoint(2));
        assertEquals(TileType.FLOOR, arena.getTile(1, 2));
    }

    @Test
    void ignoresTrailingEmptyLines() {
        Arena arena = ArenaLoader.parse(VALID_MAP + "\n\n");

        assertEquals(4, arena.getHeight());
    }

    @Test
    void acceptsWindowsLineEndings() {
        Arena arena = ArenaLoader.parse(VALID_MAP.replace("\n", "\r\n"));

        assertEquals(6, arena.getWidth());
    }

    @Test
    void rejectsEmptyMap() {
        assertRejected("   \n", "The map is empty");
        assertRejected(null, "The map is empty");
    }

    @Test
    void rejectsLinesOfDifferentLengths() {
        assertRejected("######\n#1..2#\n#...#\n######", "Line 3 has 5 tiles, expected 6");
    }

    @Test
    void rejectsUnknownSymbol() {
        assertRejected("######\n#1.x2#\n######", "Line 2, column 4: unknown symbol 'x'");
    }

    @Test
    void rejectsOpenBorder() {
        assertRejected("######\n.1..2#\n######", "Line 2, column 1: the border must be a wall");
    }

    @Test
    void rejectsSpawnPointOnBorder() {
        assertRejected("##1###\n#...2#\n######", "Line 1, column 3: the border must be a wall");
    }

    @Test
    void rejectsDuplicateSpawnPoint() {
        assertRejected("######\n#1..1#\n######", "Line 2, column 5: spawn point 1 is defined twice");
    }

    @Test
    void rejectsPlayerNumberAboveFour() {
        assertRejected("######\n#1..5#\n######", "Line 2, column 5: unknown symbol '5'");
    }

    @Test
    void rejectsMapWithFewerThanTwoSpawnPoints() {
        assertRejected("######\n#1...#\n######", "The map needs at least 2 spawn points, found 1");
    }

    @Test
    void shippedArenaIsValid() throws IOException {
        Arena arena = ArenaLoader.parse(Files.readString(Path.of(System.getProperty("assets.dir"), "maps", "arena1.txt")));

        assertEquals(16, arena.getWidth());
        assertEquals(9, arena.getHeight());
        assertEquals(4, arena.getSpawnPoints().size());
    }

    private static void assertRejected(String map, String expectedMessage) {
        ArenaFormatException error = assertThrows(ArenaFormatException.class, () -> ArenaLoader.parse(map));
        assertEquals(expectedMessage, error.getMessage());
    }
}
