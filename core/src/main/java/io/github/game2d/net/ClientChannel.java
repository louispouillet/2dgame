package io.github.game2d.net;

import io.github.game2d.protocol.LobbyState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;

/**
 * A client, as seen by the server: the server pushes snapshots and lobby states, and reads its last input.
 */
public interface ClientChannel {
    /** Sends the latest snapshot to this client. */
    void sendSnapshot(WorldSnapshot snapshot);
    /** Sends the new lobby state to this client. */
    void sendLobby(LobbyState lobby);
    /** Returns the most recent input received from this client. */
    PlayerInput latestInput();
}
