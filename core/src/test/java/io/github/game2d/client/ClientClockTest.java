package io.github.game2d.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.time.TickClock;
import org.junit.jupiter.api.Test;

class ClientClockTest {
    private static final double DELTA = 1e-6;
    private static final double TICK = 1.0 / TickClock.TICK_RATE;

    private final ClientClock clock = new ClientClock();

    // Horloge locale = temps serveur + 10 s (latence fixe comprise).
    private static double localTimeOf(int tick) {
        return 10.0 + TickClock.timeOfTick(tick);
    }

    private void observeAt(int tick, double localTime) {
        clock.update(localTime);
        clock.observe(tick);
    }

    private int inputTicks() {
        int[] count = {0};
        clock.forEachInputTick(() -> count[0]++);
        return count[0];
    }

    @Test
    void beforeAnySnapshotTheEstimateIsTheLocalTime() {
        clock.update(5.0);

        assertEquals(5.0, clock.serverTime(), DELTA);
    }

    @Test
    void firstSnapshotFixesTheOffset() {
        observeAt(30, localTimeOf(30));

        clock.update(11.5);

        // tick 30 = 1,0 s côté serveur, vu à 11,0 s côté client : l'écart est de 10 s
        assertEquals(1.5, clock.serverTime(), DELTA);
    }

    @Test
    void regularSnapshotsKeepTheEstimateExact() {
        for (int tick = 30; tick <= 90; tick++) {
            observeAt(tick, localTimeOf(tick));
        }

        clock.update(localTimeOf(90) + 0.5);

        assertEquals(3.5, clock.serverTime(), DELTA);
    }

    @Test
    void oneLateSnapshotBarelyMovesTheEstimate() {
        for (int tick = 30; tick <= 60; tick++) {
            observeAt(tick, localTimeOf(tick));
        }
        clock.update(20.0);
        double before = clock.serverTime();

        observeAt(61, localTimeOf(61) + 0.05);
        clock.update(20.0);

        assertTrue(Math.abs(clock.serverTime() - before) < 0.01);
    }

    @Test
    void estimateFollowsALastingChangeOfLatency() {
        for (int tick = 30; tick <= 60; tick++) {
            observeAt(tick, localTimeOf(tick));
        }

        for (int tick = 61; tick <= 260; tick++) {
            observeAt(tick, localTimeOf(tick) + 0.05);
        }
        clock.update(20.0);

        // le serveur est désormais vu 50 ms plus tard : l'estimation a rattrapé cet écart
        assertEquals(20.0 - 10.05, clock.serverTime(), 0.001);
    }

    @Test
    void theFirstFrameSendsNoInput() {
        clock.update(3.0);

        assertEquals(0, inputTicks());
    }

    @Test
    void sendsOneInputPerTickOfElapsedTime() {
        clock.update(3.0);
        clock.update(3.0 + 2.5 * TICK);

        assertEquals(2, inputTicks());
    }

    @Test
    void theTimeOfAFrameIsOnlyUsedOnce() {
        clock.update(3.0);
        clock.update(3.0 + 1.5 * TICK);
        inputTicks();

        assertEquals(0, inputTicks());
    }
}
