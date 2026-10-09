package io.github.game2d.time;

import java.util.function.IntConsumer;

public class TickClock {
    public static final int TICK_RATE = 30;
    public static final float STEP = 1f / TICK_RATE;
    private static final float MAX_FRAME_TIME = 0.25f;

    private final float step;
    private float accumulator;
    private int tick;

    public TickClock() {
        this(STEP);
    }

    public TickClock(float step) {
        if (step <= 0) {
            throw new IllegalArgumentException("Step must be positive");
        }
        this.step = step;
    }

    public static double timeOfTick(int tick) {
        return tick / (double) TICK_RATE;
    }

    public float getStep() {
        return step;
    }

    public int getTick() {
        return tick;
    }

    public void advance(float frameTime, IntConsumer onTick) {
        accumulator += Math.min(frameTime, MAX_FRAME_TIME);
        while (accumulator >= step) {
            accumulator -= step;
            step(onTick);
        }
    }

    public void step(IntConsumer onTick) {
        tick++;
        onTick.accept(tick);
    }
}
