package io.github.game2d.net.kryo;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import io.github.game2d.protocol.*;
import io.github.game2d.server.GameServer;
import io.github.game2d.server.LobbyException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server side of the KryoNet transport: forwards the messages of the clients to the {@link GameServer}.
 * A client must send a {@link JoinRequest} first; every other message is ignored until it has joined.
 */
public class NetworkServer extends NetworkEndpoint<Server> {
    private static final int NOT_JOINED = -1;

    /** A connection and the player behind it. */
    private static final class Client {
        private final NetworkClientChannel channel;
        private int playerId = NOT_JOINED;

        private Client(NetworkClientChannel channel) {
            this.channel = channel;
        }

        private boolean hasJoined() {
            return playerId != NOT_JOINED;
        }
    }

    private final GameServer gameServer;
    private final Map<Connection, Client> clients = new ConcurrentHashMap<>();

    /**
     * Creates a server that is not listening yet.
     *
     * @param gameServer the game to feed with the messages
     */
    public NetworkServer(GameServer gameServer) {
        super(new Server());
        this.gameServer = gameServer;
        endPoint().addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                clients.put(connection, new Client(new NetworkClientChannel(connection)));
            }

            @Override
            public void disconnected(Connection connection) {
                Client client = clients.remove(connection);
                if (client != null && client.hasJoined()) {
                    gameServer.leave(client.playerId);
                }
            }

            @Override
            public void received(Connection connection, Object object) {
                Client client = clients.get(connection);
                if (client != null) {
                    handle(connection, client, object);
                }
            }
        });
    }

    /** Dispatches one message of a client. */
    private void handle(Connection connection, Client client, Object message) {
        if (message instanceof JoinRequest request) {
            join(connection, client, request.name());
        } else if (!client.hasJoined()) {
            return;
        } else if (message instanceof PlayerInput input) {
            client.channel.receive(input);
        } else if (message instanceof ChooseTeam choice) {
            ignoreRefusal(() -> gameServer.chooseTeam(client.playerId, choice.team()));
        } else if (message instanceof ChooseBrawler choice) {
            ignoreRefusal(() -> gameServer.chooseBrawler(client.playerId, choice.brawler()));
        } else if (message instanceof ChooseMap choice) {
            ignoreRefusal(() -> gameServer.chooseMap(client.playerId, choice.map()));
        } else if (message instanceof StartRequest) {
            ignoreRefusal(() -> gameServer.requestStart(client.playerId));
        }
    }

    /** Asks the game to add the player and answers with {@link JoinAccepted} or {@link JoinRejected}. */
    private void join(Connection connection, Client client, String name) {
        if (client.hasJoined()) {
            return;
        }
        try {
            client.playerId = gameServer.join(client.channel, name);
            connection.sendTCP(new JoinAccepted(client.playerId));
        } catch (LobbyException e) {
            connection.sendTCP(new JoinRejected(e.getReason()));
        }
    }

    /**
     * Runs a request and drops its refusal: the interface already prevents it, so it can only come from an out-of-sync client.
     */
    private static void ignoreRefusal(Runnable request) {
        try {
            request.run();
        } catch (LobbyException e) {
            // The UI prevents these requests: a refusal here comes from an out-of-sync client
        }
    }

    /**
     * Starts listening. Does nothing when already running.
     *
     * @param tcpPort port for the lobby
     * @param udpPort port for the inputs and snapshots
     * @throws IOException if a port cannot be used
     */
    public void start(int tcpPort, int udpPort) throws IOException {
        if (isRunning()) {
            return;
        }
        endPoint().bind(tcpPort, udpPort);
        startUpdating();
    }

    /** Releases the KryoNet resources. */
    @Override
    protected void dispose() throws IOException {
        endPoint().dispose();
    }
}
