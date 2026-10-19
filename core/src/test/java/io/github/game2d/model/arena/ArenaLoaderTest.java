package io.github.game2d.model.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.model.Team;
import java.util.List;
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
    void acceptsSpawnPointsUpToSix() {
        Arena arena = ArenaLoader.parse("########\n#123456#\n########");

        assertEquals(6, arena.getSpawnPoints().size());
    }

    @Test
    void rejectsPlayerNumberAboveSix() {
        assertRejected("######\n#1..7#\n######", "Line 2, column 5: unknown symbol '7'");
    }

    @Test
    void rejectsMapWithFewerThanTwoSpawnPoints() {
        assertRejected("######\n#1...#\n######", "The map needs at least 2 spawn points, found 1");
    }

    @Test
    void everyShippedMapIsValidAndHasTheStandardSize() {
        for (String map : ArenaLoader.availableMaps()) {
            Arena arena = ArenaLoader.loadResource(map);

            assertEquals(23, arena.getWidth(), map);
            assertEquals(35, arena.getHeight(), map);
        }
    }

    @Test
    void availableMapsListsTheMapsFolder() {
        List<String> maps = ArenaLoader.availableMaps();

        assertTrue(maps.contains("maps/Chambre-Chorale.txt"));
        assertTrue(maps.contains("maps/Ravin-du-Bras-d-Or.txt"));
        assertTrue(maps.contains("maps/Phenix-Flamboyant.txt"));
        assertTrue(maps.contains("maps/Pinces-de-Crabe.txt"));
        assertTrue(maps.contains(ArenaLoader.DEFAULT_MAP));
        assertTrue(maps.stream().allMatch(map -> map.startsWith("maps/") && map.endsWith(".txt")));
    }

    @Test
    void everyAvailableMapHasThreeSpawnPointsPerTeam() {
        for (String map : ArenaLoader.availableMaps()) {
            Arena arena = ArenaLoader.loadResource(map);

            assertEquals(3, arena.getSpawnPoints(Team.BLUE).size(), map);
            assertEquals(3, arena.getSpawnPoints(Team.RED).size(), map);
        }
    }

    private static void assertRejected(String map, String expectedMessage) {
        ArenaFormatException error = assertThrows(ArenaFormatException.class, () -> ArenaLoader.parse(map));
        assertEquals(expectedMessage, error.getMessage());
    }

    @Test
    void loadResourceReadsTheDefaultMap() {
        Arena arena = ArenaLoader.loadResource(ArenaLoader.DEFAULT_MAP);

        assertEquals(23, arena.getWidth());
        assertEquals(35, arena.getHeight());
    }

    @Test
    void loadResourceRejectsAMissingMap() {
        assertThrows(IllegalArgumentException.class, () -> ArenaLoader.loadResource("maps/missing.txt"));
    }
}
