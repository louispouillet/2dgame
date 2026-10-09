package io.github.game2d.client;

import io.github.game2d.model.arena.Arena;
import io.github.game2d.net.LocalChannel;
import io.github.game2d.net.ServerChannel;
import io.github.game2d.server.GameServer;

public class LocalSession implements GameSession {
    private final GameServer server;
    private final LocalChannel channel = new LocalChannel();
    private final int playerId;

    public LocalSession(Arena arena) {
        server = new GameServer(arena);
        playerId = server.join(channel);
        // Adversaire immobile, pour avoir une cible en solo
        server.join(new LocalChannel());
        server.start();
    }

    @Override
    public ServerChannel channel() {
        return channel;
    }

    @Override
    public int playerId() {
        return playerId;
    }

    @Override
    public void close() {
        server.stop();
    }
}
