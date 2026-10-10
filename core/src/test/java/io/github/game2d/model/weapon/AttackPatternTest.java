package io.github.game2d.model.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AttackPatternTest {

    @Test
    void spreadFiresEveryProjectileAtOnce() {
        List<Shot> shots = new SpreadPattern(5, 10).shots();

        assertEquals(5, shots.size());
        assertTrue(shots.stream().allMatch(shot -> shot.delay() == 0 && shot.sideOffset() == 0));
    }

    @Test
    void spreadIsSymmetricAroundTheAim() {
        List<Shot> shots = new SpreadPattern(5, 10).shots();

        assertEquals(-20, shots.get(0).angle(), 1e-5);
        assertEquals(0, shots.get(2).angle(), 1e-5);
        assertEquals(20, shots.get(4).angle(), 1e-5);
    }

    @Test
    void burstSpacesItsShotsInTime() {
        List<Shot> shots = new BurstPattern(6, 0.08f, 0.15f).shots();

        assertEquals(6, shots.size());
        for (int i = 0; i < shots.size(); i++) {
            assertEquals(i * 0.08f, shots.get(i).delay(), 1e-5);
            assertEquals(0, shots.get(i).angle(), 1e-5);
        }
    }

    @Test
    void burstAlternatesRightThenLeft() {
        List<Shot> shots = new BurstPattern(6, 0.08f, 0.15f).shots();

        for (int i = 0; i < shots.size(); i++) {
            float expected = i % 2 == 0 ? 0.15f : -0.15f;
            assertEquals(expected, shots.get(i).sideOffset(), 1e-5, "shot " + i);
        }
    }

    @Test
    void patternNeedsAtLeastOneShot() {
        assertThrows(IllegalArgumentException.class, () -> new SpreadPattern(0, 10));
        assertThrows(IllegalArgumentException.class, () -> new BurstPattern(0, 0.08f, 0.15f));
    }
}
