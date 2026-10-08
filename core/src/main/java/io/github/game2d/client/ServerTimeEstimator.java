package io.github.game2d.client;

public class ServerTimeEstimator {
    private static final double SMOOTHING = 0.1;

    private final double tickRate;
    private double offset;
    private boolean ready;

    public ServerTimeEstimator(double tickRate) {
        if (tickRate <= 0) {
            throw new IllegalArgumentException("Tick rate must be positive");
        }
        this.tickRate = tickRate;
    }

    public double timeOfTick(int tick) {
        return tick / tickRate;
    }

    public void observe(int tick, double localTime) {
        double sample = localTime - timeOfTick(tick);
        offset = ready ? offset + (sample - offset) * SMOOTHING : sample;
        ready = true;
    }

    public double estimateAt(double localTime) {
        return localTime - offset;
    }
}
