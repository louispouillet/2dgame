package io.github.game2d.client;

import io.github.game2d.time.TickClock;

public class ClientClock {
    private static final double SMOOTHING = 0.1;

    private final TickClock inputClock = new TickClock();
    private double localTime;
    private float frameTime;
    private boolean started;
    private double serverOffset;
    private boolean synced;

    public void update(double localTime) {
        frameTime = started ? (float) (localTime - this.localTime) : 0f;
        this.localTime = localTime;
        started = true;
    }

    public void observe(int serverTick) {
        double sample = localTime - TickClock.timeOfTick(serverTick);
        serverOffset = synced ? serverOffset + (sample - serverOffset) * SMOOTHING : sample;
        synced = true;
    }

    public double serverTime() {
        return localTime - serverOffset;
    }

    public void forEachInputTick(Runnable onTick) {
        inputClock.advance(frameTime, tick -> onTick.run());
        frameTime = 0f;
    }
}
