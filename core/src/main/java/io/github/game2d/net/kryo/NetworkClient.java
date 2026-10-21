package io.github.game2d.net.kryo;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import io.github.game2d.protocol.JoinAccepted;
import io.github.game2d.protocol.JoinRejected;
import io.github.game2d.protocol.LobbyState;
import io.github.game2d.protocol.WorldSnapshot;
import java.io.IOException;

/**
 * Client side of the KryoNet transport. Incoming messages are stored in a {@link NetworkServerChannel}.
 */
public class NetworkClient extends NetworkEndpoint<Client> {
    private final NetworkServerChannel channel;

    /** Creates a client that is not connected yet. */
    public NetworkClient() {
        super(new Client());
        channel = new NetworkServerChannel(endPoint());
        endPoint().addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof JoinAccepted accepted) {
                    channel.accept(accepted.brawlerId());
                } else if (object instanceof JoinRejected rejected) {
                    channel.reject(rejected.reason());
                } else if (object instanceof LobbyState lobby) {
                    channel.receive(lobby);
                } else if (object instanceof WorldSnapshot received) {
                    channel.receive(received);
                }
            }
        });
    }

    /** Returns the channel used to talk to the server. */
    public NetworkServerChannel channel() {
        return channel;
    }

    /**
     * Connects to a server and starts listening (waits at most 5 seconds).
     * Does nothing when already running.
     *
     * @throws IOException if the server cannot be reached
     */
    public void connect(String host, int tcpPort, int udpPort) throws IOException {
        if (isRunning()) {
            return;
        }
        startUpdating();
        try {
            endPoint().connect(5000, host, tcpPort, udpPort);
        } catch (IOException e) {
            stop();
            throw e;
        }
    }

    /** Releases the KryoNet resources. */
    @Override
    protected void dispose() throws IOException {
        endPoint().dispose();
    }
}
