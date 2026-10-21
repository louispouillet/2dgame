package io.github.game2d.net.kryo;

import com.esotericsoftware.kryonet.EndPoint;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Common part of the KryoNet client and server: registers the messages and runs the update thread.
 *
 * @param <E> the KryoNet end point
 */
public abstract class NetworkEndpoint<E extends EndPoint> {
    private static final int UPDATE_TIMEOUT_MS = 50;

    private final E endPoint;
    private volatile boolean running;
    private Thread thread;

    /** Wraps a KryoNet end point and registers the protocol messages on it. */
    protected NetworkEndpoint(E endPoint) {
        this.endPoint = endPoint;
        NetworkRegistry.register(endPoint.getKryo());
    }

    /** Returns the wrapped KryoNet end point. */
    protected E endPoint() {
        return endPoint;
    }

    /** Whether the update thread is running. */
    protected boolean isRunning() {
        return running;
    }

    /** Starts the daemon thread that reads and dispatches the network messages. */
    protected void startUpdating() {
        running = true;
        thread = new Thread(() -> {
            while (running) {
                try {
                    endPoint.update(UPDATE_TIMEOUT_MS);
                } catch (IOException e) {
                    System.err.println(getClass().getSimpleName() + " stopped: " + e.getMessage());
                    return;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /** Frees the resources of the end point, after it was closed. */
    protected abstract void dispose() throws IOException;

    /** Stops the update thread, closes the connections and frees the resources. */
    public void stop() {
        if (thread == null) {
            return;
        }
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            endPoint.close();
            dispose();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
