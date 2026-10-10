package io.github.game2d.model.weapon;

import java.util.ArrayList;
import java.util.List;

/** Toutes les balles partent en même temps, en éventail autour de la visée. */
public class SpreadPattern implements AttackPattern {
    private final List<Shot> shots;

    public SpreadPattern(int count, float spreadAngle) {
        if (count < 1) {
            throw new IllegalArgumentException("A pattern needs at least one shot");
        }
        List<Shot> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            float offset = i - (count - 1) / 2f;
            list.add(new Shot(0, 0, offset * spreadAngle));
        }
        shots = List.copyOf(list);
    }

    @Override
    public List<Shot> shots() {
        return shots;
    }
}
