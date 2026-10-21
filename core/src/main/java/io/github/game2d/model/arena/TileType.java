package io.github.game2d.model.arena;

import java.util.Optional;

/** The kinds of tile of a map, with their rules. */
public enum TileType {
    FLOOR('.', false, false, false),
    WALL('#', true, true, false),
    BUSH('~', false, false, true),
    WATER('W', true, false, false);

    private final char symbol;
    private final boolean blocksMovement;
    private final boolean blocksShots;
    private final boolean hidesBrawler;

    /**
     * Creates a tile type.
     *
     * @param symbol         character used in the map files
     * @param blocksMovement whether brawlers cannot walk on it
     * @param blocksShots    whether bullets stop on it
     * @param hidesBrawler   whether a brawler standing on it is hidden from far enemies
     */
    TileType(char symbol, boolean blocksMovement, boolean blocksShots, boolean hidesBrawler) {
        this.symbol = symbol;
        this.blocksMovement = blocksMovement;
        this.blocksShots = blocksShots;
        this.hidesBrawler = hidesBrawler;
    }

    /** Whether brawlers cannot walk on this tile. */
    public boolean blocksMovement() {
        return blocksMovement;
    }

    /** Whether bullets stop on this tile. */
    public boolean blocksShots() {
        return blocksShots;
    }

    /** Whether a brawler standing on this tile can be hidden from far enemies. */
    public boolean hidesBrawler() {
        return hidesBrawler;
    }

    /**
     * Returns the tile type written with this symbol in a map file, or empty if the symbol is unknown.
     */
    public static Optional<TileType> fromSymbol(char symbol) {
        for (TileType type : values()) {
            if (type.symbol == symbol) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
