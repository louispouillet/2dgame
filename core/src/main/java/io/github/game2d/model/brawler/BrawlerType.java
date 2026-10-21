package io.github.game2d.model.brawler;

/**
 * The brawlers a player can choose. Adding a value here (with its class) makes it appear in the lobby.
 */
public enum BrawlerType {
    SHELLY(Shelly::new),
    COLT(Colt::new);

    /** Builds a brawler of one type. */
    @FunctionalInterface
    private interface Factory {
        Brawler create(int id, float x, float y);
    }

    private final Factory factory;

    BrawlerType(Factory factory) {
        this.factory = factory;
    }

    /**
     * Creates a brawler of this type.
     *
     * @param id unique id in the world
     * @param x  horizontal start position
     * @param y  vertical start position
     */
    public Brawler create(int id, float x, float y) {
        return factory.create(id, x, y);
    }
}
