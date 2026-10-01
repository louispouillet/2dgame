package io.github.game2d.brawler;
import io.github.game2d.brawler.Brawler;
import io.github.game2d.weapon.Revolver;

public class Colt extends Brawler {
    public Colt(float x, float y) {
        super(x, y, 0.5f, 0.5f, 5f, 100, new Revolver());
    }
}