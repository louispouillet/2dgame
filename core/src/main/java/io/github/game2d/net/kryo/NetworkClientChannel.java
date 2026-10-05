package io.github.game2d.net.kryo;

import com.esotericsoftware.kryonet.Connection;
import io.github.game2d.net.ClientChannel;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.concurrent.atomic.AtomicReference;

public class NetworkClientChannel implements ClientChannel {
    private final Connection connection;
    private final AtomicReference<PlayerInput> input = new AtomicReference<>(new PlayerInput(0, 0, 0, 0, false));

    public NetworkClientChannel(Connection connection) {
        this.connection = connection;
    }

    @Override
    public PlayerInput latestInput() {
        return input.get();
    }

    @Override
    public void sendSnapshot(WorldSnapshot snapshot) {
        connection.sendTCP(snapshot);
    }

    public void receive(PlayerInput input) {
        this.input.set(input);
    }
}
