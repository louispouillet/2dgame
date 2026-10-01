package io.github.game2d.brawler;
import io.github.game2d.brawler.Brawler;
import io.github.game2d.weapon.Shotgun;

public class Shelly extends Brawler {
    public Shelly(float x, float y) {
        super(x, y, 0.5f, 0.5f, 4f, 120, new Shotgun());
    }
}