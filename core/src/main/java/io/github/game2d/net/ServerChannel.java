package io.github.game2d.net;

import io.github.game2d.protocol.LobbyState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;

/**
 * The server, as seen by a client: the client pushes its inputs and reads the latest snapshot and lobby state.
 */
public interface ServerChannel {
    /** Sends an input to the server. */
    void sendInput(PlayerInput input);
    /** Returns the most recent snapshot, or {@code null} if none arrived yet. */
    WorldSnapshot latestSnapshot();
    /** Returns the most recent lobby state, or {@code null} if none arrived yet. */
    LobbyState latestLobby();
}
