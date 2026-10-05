package io.github.game2d.net.kryo;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import io.github.game2d.protocol.JoinAccepted;
import io.github.game2d.protocol.WorldSnapshot;
import java.io.IOException;
import java.io.UncheckedIOException;

public class NetworkClient {
    private final Client client = new Client();
    private final NetworkServerChannel channel = new NetworkServerChannel(client);
    private volatile boolean running;
    private Thread thread;

    public NetworkClient() {
        NetworkRegistry.register(client.getKryo());
        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof JoinAccepted accepted) {
                    channel.accept(accepted.brawlerId());
                } else if (object instanceof WorldSnapshot received) {
                    channel.receive(received);
                }
            }
        });
    }

    public NetworkServerChannel channel() {
        return channel;
    }

    public void connect(String host, int tcpPort, int udpPort) throws IOException {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(() -> {
            while (running) {
                try {
                    client.update(50);
                } catch (IOException e) {
                    System.err.println("Network client stopped: " + e.getMessage());
                    return;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

        try {
            client.connect(5000, host, tcpPort, udpPort);
        } catch (IOException e) {
            stop();
            throw e;
        }
    }

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
            client.close();
            client.dispose();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
