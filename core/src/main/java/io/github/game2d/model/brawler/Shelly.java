package io.github.game2d.model.brawler;

import io.github.game2d.model.collision.Circle;
import io.github.game2d.model.weapon.Shotgun;

public class Shelly extends Brawler {
    public Shelly(int id, float x, float y) {
        super(id, x, y, new Circle(0.25f), 4f, 120, new Shotgun());
    }
}
