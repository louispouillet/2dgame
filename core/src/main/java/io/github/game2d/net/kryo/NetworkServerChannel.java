package io.github.game2d.net.kryo;

import com.esotericsoftware.kryonet.Client;
import io.github.game2d.net.ServerChannel;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.concurrent.atomic.AtomicReference;

public class NetworkServerChannel implements ServerChannel {
    private final Client client;
    private final AtomicReference<WorldSnapshot> snapshot = new AtomicReference<>();
    private volatile int brawlerId = -1;

    NetworkServerChannel(Client client) {
        this.client = client;
    }

    @Override
    public void sendInput(PlayerInput input) {
        client.sendTCP(input);
    }

    @Override
    public WorldSnapshot latestSnapshot() {
        return snapshot.get();
    }

    public int getBrawlerId() {
        return brawlerId;
    }

    void receive(WorldSnapshot snapshot) {
        this.snapshot.set(snapshot);
    }

    void accept(int brawlerId) {
        this.brawlerId = brawlerId;
    }
}
