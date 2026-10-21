package io.github.game2d.model.brawler;

import io.github.game2d.model.weapon.Revolver;

/** Fast and fragile, shoots long bursts with a revolver. */
public class Colt extends Brawler {
    /** Creates a Colt at a position. */
    public Colt(int id, float x, float y) {
        super(id, x, y, 0.4f, 3.5f, 6200, new Revolver());
    }

    @Override
    public BrawlerType getType() {
        return BrawlerType.COLT;
    }
}
