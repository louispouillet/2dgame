package io.github.game2d.net.kryo;

import com.esotericsoftware.kryonet.Client;
import io.github.game2d.model.Team;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.net.ServerChannel;
import io.github.game2d.protocol.ChooseBrawler;
import io.github.game2d.protocol.ChooseMap;
import io.github.game2d.protocol.ChooseTeam;
import io.github.game2d.protocol.JoinRequest;
import io.github.game2d.protocol.LobbyState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.RefusalReason;
import io.github.game2d.protocol.StartRequest;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The server, as seen by a network client: commands go out by TCP, inputs by UDP, and the latest snapshot,
 * lobby state and refusal are kept for the game to read.
 */
public class NetworkServerChannel implements ServerChannel {
    private final Client client;
    private final AtomicReference<WorldSnapshot> snapshot = new AtomicReference<>();
    private final AtomicReference<LobbyState> lobby = new AtomicReference<>();
    private final AtomicReference<RefusalReason> rejection = new AtomicReference<>();
    private volatile int brawlerId = -1;

    /** Creates the channel of one KryoNet client. */
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

    /** Returns why the last join was refused, or {@code null}. */
    public RefusalReason rejection() {
        return rejection.get();
    }

    /** Returns the id given by the server once the join is accepted, or -1. */
    public int getBrawlerId() {
        return brawlerId;
    }

    /** Asks to enter the lobby and forgets the previous refusal. */
    public void sendJoin(String name) {
        rejection.set(null);
        client.sendTCP(new JoinRequest(name));
    }

    /** Asks to change team. */
    public void sendChooseTeam(Team team) {
        client.sendTCP(new ChooseTeam(team));
    }

    /** Asks to play another brawler. */
    public void sendChooseBrawler(BrawlerType brawler) {
        client.sendTCP(new ChooseBrawler(brawler));
    }

    /** Asks to change the map (leader only). */
    public void sendChooseMap(String map) {
        client.sendTCP(new ChooseMap(map));
    }

    /** Asks to start the match (leader only). */
    public void sendStart() {
        client.sendTCP(new StartRequest());
    }

    /** Keeps a snapshot unless it is older than the current one (UDP may reorder). */
    void receive(WorldSnapshot snapshot) {
        WorldSnapshot current = this.snapshot.get();
        if (current == null || snapshot.tick() > current.tick()) {
            this.snapshot.set(snapshot);
        }
    }

    /** Keeps the latest lobby state. */
    void receive(LobbyState lobby) {
        this.lobby.set(lobby);
    }

    /** Stores the id given by the server. */
    void accept(int brawlerId) {
        this.brawlerId = brawlerId;
    }

    /** Stores the reason of a refused join. */
    void reject(RefusalReason reason) {
        rejection.set(reason);
    }
}
