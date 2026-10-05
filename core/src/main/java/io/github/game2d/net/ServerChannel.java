package io.github.game2d.net;

import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;

public interface ServerChannel {
    void sendInput(PlayerInput input);
    WorldSnapshot latestSnapshot();
}
