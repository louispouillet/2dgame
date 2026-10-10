package io.github.game2d.model.brawler;

import io.github.game2d.model.weapon.Shotgun;

public class Shelly extends Brawler {
    public Shelly(int id, float x, float y) {
        super(id, x, y, 0.25f, 3.5f, 120, new Shotgun());
    }

    @Override
    public BrawlerType getType() {
        return BrawlerType.SHELLY;
    }
}
