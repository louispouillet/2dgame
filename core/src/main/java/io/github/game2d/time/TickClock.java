package io.github.game2d.time;

import java.util.function.IntConsumer;

/**
 * Fixed time step shared by server and client: turns variable frame times into ticks of constant length.
 */
public class TickClock {
    /** Ticks per second. */
    public static final int TICK_RATE = 30;
    /** Duration of a tick, in seconds. */
    public static final float STEP = 1f / TICK_RATE;
    private static final float MAX_FRAME_TIME = 0.25f;

    private final float step;
    private float accumulator;
    private int tick;

    /** Creates a clock with the standard step. */
    public TickClock() {
        this(STEP);
    }

    /**
     * Creates a clock with a custom step.
     *
     * @param step seconds per tick
     * @throws IllegalArgumentException if the step is not positive
     */
    public TickClock(float step) {
        if (step <= 0) {
            throw new IllegalArgumentException("Step must be positive");
        }
        this.step = step;
    }

    /** Returns the time, in seconds, at which a tick happens. */
    public static double timeOfTick(int tick) {
        return tick / (double) TICK_RATE;
    }

    /** Returns the duration of a tick, in seconds. */
    public float getStep() {
        return step;
    }

    /** Returns the number of the last tick, starting at 0. */
    public int getTick() {
        return tick;
    }

    /**
     * Adds the time of a frame and runs as many ticks as it contains.
     * A very long frame is capped, to avoid a spiral of catching up.
     *
     * @param frameTime seconds since the last call
     * @param onTick    called with the number of each tick
     */
    public void advance(float frameTime, IntConsumer onTick) {
        accumulator += Math.min(frameTime, MAX_FRAME_TIME);
        while (accumulator >= step) {
            accumulator -= step;
            step(onTick);
        }
    }

    /** Runs exactly one tick, whatever the elapsed time. */
    public void step(IntConsumer onTick) {
        tick++;
        onTick.accept(tick);
    }
}
