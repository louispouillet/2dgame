package io.github.game2d.net;

import io.github.game2d.protocol.LobbyState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;

public interface ClientChannel {
    void sendSnapshot(WorldSnapshot snapshot);
    void sendLobby(LobbyState lobby);
    PlayerInput latestInput();
}
