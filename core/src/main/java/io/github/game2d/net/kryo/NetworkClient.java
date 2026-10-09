package io.github.game2d.net.kryo;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import io.github.game2d.protocol.JoinAccepted;
import io.github.game2d.protocol.WorldSnapshot;
import java.io.IOException;

public class NetworkClient extends NetworkEndpoint<Client> {
    private final NetworkServerChannel channel;

    public NetworkClient() {
        super(new Client());
        channel = new NetworkServerChannel(endPoint());
        endPoint().addListener(new Listener() {
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

    @Override
    protected void dispose() throws IOException {
        endPoint().dispose();
    }
}
