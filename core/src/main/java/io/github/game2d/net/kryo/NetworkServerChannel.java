package io.github.game2d.net.kryo;

import com.esotericsoftware.kryonet.Client;
import io.github.game2d.model.Team;
import io.github.game2d.net.ServerChannel;
import io.github.game2d.protocol.ChooseMap;
import io.github.game2d.protocol.ChooseTeam;
import io.github.game2d.protocol.JoinRequest;
import io.github.game2d.protocol.LobbyState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.RefusalReason;
import io.github.game2d.protocol.StartRequest;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.concurrent.atomic.AtomicReference;

public class NetworkServerChannel implements ServerChannel {
    private final Client client;
    private final AtomicReference<WorldSnapshot> snapshot = new AtomicReference<>();
    private final AtomicReference<LobbyState> lobby = new AtomicReference<>();
    private final AtomicReference<RefusalReason> rejection = new AtomicReference<>();
    private volatile int brawlerId = -1;

    NetworkServerChannel(Client client) {
        this.client = client;
    }

    @Override
    public void sendInput(PlayerInput input) {
        client.sendUDP(input);
    }

    @Override
    public WorldSnapshot latestSnapshot() {
        return snapshot.get();
    }

    @Override
    public LobbyState latestLobby() {
        return lobby.get();
    }

    public RefusalReason rejection() {
        return rejection.get();
    }

    public int getBrawlerId() {
        return brawlerId;
    }

    public void sendJoin(String name) {
        rejection.set(null);
        client.sendTCP(new JoinRequest(name));
    }

    public void sendChooseTeam(Team team) {
        client.sendTCP(new ChooseTeam(team));
    }

    public void sendChooseMap(String map) {
        client.sendTCP(new ChooseMap(map));
    }

    public void sendStart() {
        client.sendTCP(new StartRequest());
    }

    void receive(WorldSnapshot snapshot) {
        WorldSnapshot current = this.snapshot.get();
        if (current == null || snapshot.tick() > current.tick()) {
            this.snapshot.set(snapshot);
        }
    }

    void receive(LobbyState lobby) {
        this.lobby.set(lobby);
    }

    void accept(int brawlerId) {
        this.brawlerId = brawlerId;
    }

    void reject(RefusalReason reason) {
        rejection.set(reason);
    }
}
