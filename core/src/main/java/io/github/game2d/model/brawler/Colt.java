package io.github.game2d.model.brawler;

import io.github.game2d.model.collision.Circle;
import io.github.game2d.model.weapon.Revolver;

public class Colt extends Brawler {
    public Colt(int id, float x, float y) {
        super(id, x, y, new Circle(0.25f), 5f, 100, new Revolver());
    }
}
