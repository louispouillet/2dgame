package io.github.game2d.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FixedTimestepTest {
    // 0.125 et 0.0625 sont exacts en binaire : pas d'erreur d'arrondi dans les tests
    private static final float STEP = 0.125f;

    private static int advance(FixedTimestep timestep, float frameTime) {
        int[] ticks = {0};
        timestep.advance(frameTime, () -> ticks[0]++);
        return ticks[0];
    }

    @Test
    void doesNotTickBeforeAFullStepHasElapsed() {
        FixedTimestep timestep = new FixedTimestep(STEP);

        assertEquals(0, advance(timestep, 0.0625f));
    }

    @Test
    void accumulatesPartialFramesUntilAStepIsReached() {
        FixedTimestep timestep = new FixedTimestep(STEP);

        assertEquals(0, advance(timestep, 0.0625f));
        assertEquals(1, advance(timestep, 0.0625f));
    }

    @Test
    void ticksSeveralTimesForALongFrame() {
        FixedTimestep timestep = new FixedTimestep(STEP);

        assertEquals(2, advance(timestep, 0.25f));
    }

    @Test
    void keepsTheRemainderForTheNextFrame() {
        FixedTimestep timestep = new FixedTimestep(STEP);

        assertEquals(1, advance(timestep, 0.1875f));
        assertEquals(1, advance(timestep, 0.0625f));
    }

    @Test
    void clampsHugeFramesToAvoidASpiralOfDeath() {
        FixedTimestep timestep = new FixedTimestep(STEP);

        assertEquals(2, advance(timestep, 60.0f));
    }

    @Test
    void exposesTheStepForTheSimulation() {
        assertEquals(0.125f, new FixedTimestep(STEP).getStep());
    }

    @Test
    void rejectsNonPositiveStep() {
        assertThrows(IllegalArgumentException.class, () -> new FixedTimestep(0f));
        assertThrows(IllegalArgumentException.class, () -> new FixedTimestep(-1f));
    }
}
