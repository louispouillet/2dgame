package io.github.game2d.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TickClockTest {
    // 0.125 and 0.0625 are exact in binary: no rounding error in the tests
    private static final float STEP = 0.125f;

    private static List<Integer> advance(TickClock clock, float frameTime) {
        List<Integer> ticks = new ArrayList<>();
        clock.advance(frameTime, ticks::add);
        return ticks;
    }

    @Test
    void doesNotTickBeforeAFullStepHasElapsed() {
        TickClock clock = new TickClock(STEP);

        assertEquals(List.of(), advance(clock, 0.0625f));
    }

    @Test
    void accumulatesPartialFramesUntilAStepIsReached() {
        TickClock clock = new TickClock(STEP);

        assertEquals(List.of(), advance(clock, 0.0625f));
        assertEquals(List.of(1), advance(clock, 0.0625f));
    }

    @Test
    void ticksSeveralTimesForALongFrame() {
        TickClock clock = new TickClock(STEP);

        assertEquals(List.of(1, 2), advance(clock, 0.25f));
    }

    @Test
    void keepsTheRemainderForTheNextFrame() {
        TickClock clock = new TickClock(STEP);

        assertEquals(List.of(1), advance(clock, 0.1875f));
        assertEquals(List.of(2), advance(clock, 0.0625f));
    }

    @Test
    void clampsHugeFramesToAvoidASpiralOfDeath() {
        TickClock clock = new TickClock(STEP);

        assertEquals(2, advance(clock, 60.0f).size());
    }

    @Test
    void stepTicksOnceRegardlessOfElapsedTime() {
        TickClock clock = new TickClock(STEP);
        List<Integer> ticks = new ArrayList<>();

        clock.step(ticks::add);
        clock.step(ticks::add);

        assertEquals(List.of(1, 2), ticks);
        assertEquals(2, clock.getTick());
    }

    @Test
    void stepAndAdvanceShareTheSameCounter() {
        TickClock clock = new TickClock(STEP);
        clock.step(tick -> { });

        assertEquals(List.of(2), advance(clock, STEP));
    }

    @Test
    void exposesTheStepForTheSimulation() {
        assertEquals(0.125f, new TickClock(STEP).getStep());
        assertEquals(TickClock.STEP, new TickClock().getStep());
    }

    @Test
    void timeOfATickIsItsNumberDividedByTheTickRate() {
        assertEquals(1.0, TickClock.timeOfTick(TickClock.TICK_RATE), 1e-9);
    }

    @Test
    void rejectsNonPositiveStep() {
        assertThrows(IllegalArgumentException.class, () -> new TickClock(0f));
        assertThrows(IllegalArgumentException.class, () -> new TickClock(-1f));
    }
}
