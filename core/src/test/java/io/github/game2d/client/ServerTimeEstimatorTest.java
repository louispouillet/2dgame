package io.github.game2d.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ServerTimeEstimatorTest {
    private static final double TICK_RATE = 30;
    private static final double DELTA = 1e-6;

    // Horloge locale = temps serveur + 10 s (latence fixe comprise).
    private static double localTimeOf(int tick) {
        return 10.0 + tick / TICK_RATE;
    }

    @Test
    void tickRateMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new ServerTimeEstimator(0));
    }

    @Test
    void timeOfATickIsItsNumberDividedByTheTickRate() {
        assertEquals(1.0, new ServerTimeEstimator(TICK_RATE).timeOfTick(30), DELTA);
    }

    @Test
    void beforeAnySnapshotTheEstimateIsTheLocalTime() {
        ServerTimeEstimator estimator = new ServerTimeEstimator(TICK_RATE);

        assertEquals(5.0, estimator.estimateAt(5.0), DELTA);
    }

    @Test
    void firstSnapshotFixesTheOffset() {
        ServerTimeEstimator estimator = new ServerTimeEstimator(TICK_RATE);

        estimator.observe(30, localTimeOf(30));

        // tick 30 = 1,0 s côté serveur, vu à 11,0 s côté client : l'écart est de 10 s
        assertEquals(1.5, estimator.estimateAt(11.5), DELTA);
    }

    @Test
    void regularSnapshotsKeepTheEstimateExact() {
        ServerTimeEstimator estimator = new ServerTimeEstimator(TICK_RATE);

        for (int tick = 30; tick <= 90; tick++) {
            estimator.observe(tick, localTimeOf(tick));
        }

        assertEquals(3.5, estimator.estimateAt(localTimeOf(90) + 0.5), DELTA);
    }

    @Test
    void oneLateSnapshotBarelyMovesTheEstimate() {
        ServerTimeEstimator estimator = new ServerTimeEstimator(TICK_RATE);
        for (int tick = 30; tick <= 60; tick++) {
            estimator.observe(tick, localTimeOf(tick));
        }
        double before = estimator.estimateAt(20.0);

        estimator.observe(61, localTimeOf(61) + 0.05);

        assertTrue(Math.abs(estimator.estimateAt(20.0) - before) < 0.01);
    }

    @Test
    void estimateFollowsALastingChangeOfLatency() {
        ServerTimeEstimator estimator = new ServerTimeEstimator(TICK_RATE);
        for (int tick = 30; tick <= 60; tick++) {
            estimator.observe(tick, localTimeOf(tick));
        }

        for (int tick = 61; tick <= 260; tick++) {
            estimator.observe(tick, localTimeOf(tick) + 0.05);
        }

        // le serveur est désormais vu 50 ms plus tard : l'estimation a rattrapé cet écart
        assertEquals(20.0 - 10.05, estimator.estimateAt(20.0), 0.001);
    }
}
