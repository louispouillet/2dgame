package io.github.game2d.model.brawler;

public enum BrawlerType {
    SHELLY(Shelly::new),
    COLT(Colt::new);

    @FunctionalInterface
    private interface Factory {
        Brawler create(int id, float x, float y);
    }

    private final Factory factory;

    BrawlerType(Factory factory) {
        this.factory = factory;
    }

    public Brawler create(int id, float x, float y) {
        return factory.create(id, x, y);
    }
}
