package io.github.game2d.model.brawler;

import io.github.game2d.model.weapon.Revolver;

public class Colt extends Brawler {
    public Colt(int id, float x, float y) {
        super(id, x, y, 0.4f, 3f, 100, new Revolver());
    }

    @Override
    public BrawlerType getType() {
        return BrawlerType.COLT;
    }
}
