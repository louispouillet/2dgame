package io.github.game2d.net;

import io.github.game2d.protocol.LobbyState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory transport for the solo mode: client and server share this object, nothing goes through the network.
 * Each direction keeps only the latest value.
 */
public class LocalChannel implements ClientChannel, ServerChannel {
    private final AtomicReference<PlayerInput> input = new AtomicReference<>(new PlayerInput(0, 0, 0, 0, 0, false));
    private final AtomicReference<WorldSnapshot> snapshot = new AtomicReference<>();
    private final AtomicReference<LobbyState> lobby = new AtomicReference<>();

    @Override
    public void sendInput(PlayerInput input) {
        this.input.set(input);
    }

    @Override
    public PlayerInput latestInput() {
        return input.get();
    }

    @Override
    public void sendSnapshot(WorldSnapshot snapshot) {
        this.snapshot.set(snapshot);
    }

    @Override
    public WorldSnapshot latestSnapshot() {
        return snapshot.get();
    }

    @Override
    public void sendLobby(LobbyState lobby) {
        this.lobby.set(lobby);
    }

    @Override
    public LobbyState latestLobby() {
        return lobby.get();
    }
}
