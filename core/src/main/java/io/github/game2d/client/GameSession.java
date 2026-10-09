package io.github.game2d.client;

import io.github.game2d.net.ServerChannel;

public interface GameSession {
    ServerChannel channel();

    int playerId();

    void close();
}
