package io.github.game2d.brawler;
import io.github.game2d.collision.Circle;
import io.github.game2d.weapon.Shotgun;

public class Shelly extends Brawler {
    public Shelly(float x, float y) {
        super(x, y, new Circle(0.25f), 4f, 120, new Shotgun());
    }
}
