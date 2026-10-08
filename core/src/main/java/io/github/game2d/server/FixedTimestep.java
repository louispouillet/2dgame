package io.github.game2d.server;

public class FixedTimestep {
    private static final float MAX_FRAME_TIME = 0.25f;

    private final float step;
    private float accumulator;

    public FixedTimestep(float step) {
        if (step <= 0) {
            throw new IllegalArgumentException("Step must be positive");
        }
        this.step = step;
    }

    public float getStep() {
        return step;
    }

    public void advance(float frameTime, Runnable tick) {
        accumulator += Math.min(frameTime, MAX_FRAME_TIME);
        while (accumulator >= step) {
            tick.run();
            accumulator -= step;
        }
    }
}
