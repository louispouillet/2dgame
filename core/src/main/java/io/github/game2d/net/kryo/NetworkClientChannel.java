package io.github.game2d.net.kryo;

import com.esotericsoftware.kryonet.Connection;
import io.github.game2d.net.ClientChannel;
import io.github.game2d.protocol.LobbyState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A connected client, as seen by the server: snapshots go by UDP, lobby states by TCP.
 * Only the newest input is kept; late or duplicated inputs are ignored.
 */
public class NetworkClientChannel implements ClientChannel {
    private final Connection connection;
    private final AtomicReference<PlayerInput> input = new AtomicReference<>(new PlayerInput(0, 0, 0, 0, 0, false));

    /** Creates the channel of one connection. */
    public NetworkClientChannel(Connection connection) {
        this.connection = connection;
    }

    @Override
    public PlayerInput latestInput() {
        return input.get();
    }

    @Override
    public void sendSnapshot(WorldSnapshot snapshot) {
        connection.sendUDP(snapshot);
    }

    @Override
    public void sendLobby(LobbyState lobby) {
        connection.sendTCP(lobby);
    }

    /** Stores an input received from the network, unless an older one. */
    public void receive(PlayerInput input) {
        if (input.sequence() > this.input.get().sequence()) {
            this.input.set(input);
        }
    }
}
