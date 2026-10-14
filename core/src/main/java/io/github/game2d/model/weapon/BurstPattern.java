package io.github.game2d.model.weapon;

import java.util.ArrayList;
import java.util.List;

/** Bullets leave one after another, alternately right then left of the aim (two guns). */
public class BurstPattern implements AttackPattern {
    private final List<Shot> shots;

    public BurstPattern(int count, float interval, float sideOffset) {
        if (count < 1) {
            throw new IllegalArgumentException("A pattern needs at least one shot");
        }
        List<Shot> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            float side = i % 2 == 0 ? sideOffset : -sideOffset;
            list.add(new Shot(i * interval, side, 0));
        }
        shots = List.copyOf(list);
    }

    @Override
    public List<Shot> shots() {
        return shots;
    }
}
