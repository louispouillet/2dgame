package io.github.game2d.net.kryo;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import io.github.game2d.net.ClientChannel;
import io.github.game2d.protocol.JoinAccepted;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.server.GameServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkServer {
    private final Server server = new Server();
    private record Client(NetworkClientChannel channel, int brawlerId) {}
    private final Map<Connection, Client> clients = new ConcurrentHashMap<>();
    private volatile boolean running;
    private Thread thread;

    public NetworkServer(GameServer gameServer) {
        NetworkRegistry.register(server.getKryo());
        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                NetworkClientChannel networkClientChannel = new NetworkClientChannel(connection);
                int id = gameServer.join(networkClientChannel);
                clients.put(connection, new Client(networkClientChannel, id));
                connection.sendTCP(new JoinAccepted(id));
            }

            @Override
            public void disconnected(Connection connection) {
                Client client = clients.remove(connection);
                if (client != null) {
                    gameServer.leave(client.brawlerId());
                }
            }

            @Override
            public void received(Connection connection, Object object) {
                Client client = clients.get(connection);
                if (object instanceof PlayerInput input && client != null) {
                    client.channel().receive(input);
                }
            }
        });
    }

    public void start(int tcpPort, int udpPort) throws IOException {
        if (running) {
            return;
        }
        server.bind(tcpPort, udpPort);
        running = true;
        thread = new Thread(() -> {
            while (running) {
                try {
                    server.update(50);
                } catch (IOException e) {
                    System.err.println("Network server stopped: " + e.getMessage());
                    return;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
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
            server.close();
            server.dispose();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
