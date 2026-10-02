package io.github.game2d.brawler;
import io.github.game2d.collision.Circle;
import io.github.game2d.weapon.Revolver;

public class Colt extends Brawler {
    public Colt(float x, float y) {
        super(x, y, new Circle(0.25f), 5f, 100, new Revolver());
    }
}
