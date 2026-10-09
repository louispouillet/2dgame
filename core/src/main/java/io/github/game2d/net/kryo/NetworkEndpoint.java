package io.github.game2d.net.kryo;

import com.esotericsoftware.kryonet.EndPoint;
import java.io.IOException;
import java.io.UncheckedIOException;

public abstract class NetworkEndpoint<E extends EndPoint> {
    private static final int UPDATE_TIMEOUT_MS = 50;

    private final E endPoint;
    private volatile boolean running;
    private Thread thread;

    protected NetworkEndpoint(E endPoint) {
        this.endPoint = endPoint;
        NetworkRegistry.register(endPoint.getKryo());
    }

    protected E endPoint() {
        return endPoint;
    }

    protected boolean isRunning() {
        return running;
    }

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

    protected abstract void dispose() throws IOException;

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
