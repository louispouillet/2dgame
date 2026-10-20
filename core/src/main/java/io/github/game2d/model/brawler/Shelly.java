package io.github.game2d.model.brawler;

import io.github.game2d.model.weapon.Shotgun;

/** Tanky and slower, shoots a fan of pellets at short range with a shotgun. */
public class Shelly extends Brawler {
    /** Creates a Shelly at a position. */
    public Shelly(int id, float x, float y) {
        super(id, x, y, 0.4f, 3.0f, 7800, new Shotgun());
    }

    @Override
    public BrawlerType getType() {
        return BrawlerType.SHELLY;
    }
}
