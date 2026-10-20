package io.github.game2d.client;

import io.github.game2d.time.TickClock;

/**
 * The only clock of the client: local time, an estimate of the server time and the pace of the inputs.
 */
public class ClientClock {
    private static final double SMOOTHING = 0.1;

    private final TickClock inputClock = new TickClock();
    private double localTime;
    private float frameTime;
    private boolean started;
    private double serverOffset;
    private boolean synced;

    /**
     * Gives the current local time, once per frame.
     *
     * @param localTime seconds since any fixed moment
     */
    public void update(double localTime) {
        frameTime = started ? (float) (localTime - this.localTime) : 0f;
        this.localTime = localTime;
        started = true;
    }

    /**
     * Refines the estimate of the server time with the tick of a new snapshot (smoothed, to absorb the network jitter).
     */
    public void observe(int serverTick) {
        double sample = localTime - TickClock.timeOfTick(serverTick);
        serverOffset = synced ? serverOffset + (sample - serverOffset) * SMOOTHING : sample;
        synced = true;
    }

    /** Returns the estimated time on the server, in seconds. */
    public double serverTime() {
        return localTime - serverOffset;
    }

    /** Runs the action once for each input tick (30 per second) that elapsed since the last frame. */
    public void forEachInputTick(Runnable onTick) {
        inputClock.advance(frameTime, tick -> onTick.run());
        frameTime = 0f;
    }
}
