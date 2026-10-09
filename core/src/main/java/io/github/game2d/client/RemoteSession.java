package io.github.game2d.client;

import io.github.game2d.net.ServerChannel;
import io.github.game2d.net.kryo.NetworkClient;
import io.github.game2d.net.kryo.NetworkRegistry;
import java.io.IOException;

public class RemoteSession implements GameSession {
    private final NetworkClient client = new NetworkClient();

    public RemoteSession(String host) throws IOException {
        client.connect(host, NetworkRegistry.DEFAULT_TCP_PORT, NetworkRegistry.DEFAULT_UDP_PORT);
    }

    @Override
    public ServerChannel channel() {
        return client.channel();
    }

    @Override
    public int playerId() {
        return client.channel().getBrawlerId();
    }

    @Override
    public void close() {
        client.stop();
    }
}
