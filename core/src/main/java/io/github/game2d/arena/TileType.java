package io.github.game2d.arena;

import java.util.Optional;

public enum TileType {
    FLOOR('.', false, false, false),
    WALL('#', true, true, false),
    BUSH('~', false, false, true),
    WATER('W', true, false, false);

    private final char symbol;
    private final boolean blocksMovement;
    private final boolean blocksShots;
    private final boolean hidesBrawler;

    TileType(char symbol, boolean blocksMovement, boolean blocksShots, boolean hidesBrawler) {
        this.symbol = symbol;
        this.blocksMovement = blocksMovement;
        this.blocksShots = blocksShots;
        this.hidesBrawler = hidesBrawler;
    }

    public char getSymbol() {
        return symbol;
    }

    public boolean blocksMovement() {
        return blocksMovement;
    }

    public boolean blocksShots() {
        return blocksShots;
    }

    public boolean hidesBrawler() {
        return hidesBrawler;
    }

    public static Optional<TileType> fromSymbol(char symbol) {
        for (TileType type : values()) {
            if (type.symbol == symbol) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
