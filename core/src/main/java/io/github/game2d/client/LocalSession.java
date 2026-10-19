package io.github.game2d.client;

import io.github.game2d.model.Team;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.ArenaLoader;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.net.LocalChannel;
import io.github.game2d.net.ServerChannel;
import io.github.game2d.protocol.RefusalReason;
import io.github.game2d.server.GameServer;
import io.github.game2d.server.LobbyException;
import java.util.List;

public class LocalSession implements GameSession {
    private static final String MAP = ArenaLoader.DEFAULT_MAP;
    private static final String TARGET_NAME = "Target";
    private static final int NOT_JOINED = -1;

    private final GameServer server;
    private final LocalChannel channel = new LocalChannel();
    private int playerId = NOT_JOINED;
    private RefusalReason rejection;

    public LocalSession(Arena arena) {
        server = new GameServer(List.of(MAP), MAP, map -> arena);
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
    public void join(String name) {
        try {
            playerId = server.join(channel, name);
            rejection = null;
            // Still opponent, so solo has a target
            server.join(new LocalChannel(), TARGET_NAME.equalsIgnoreCase(name.strip()) ? TARGET_NAME + "2" : TARGET_NAME);
        } catch (LobbyException e) {
            rejection = e.getReason();
        }
    }

    @Override
    public void chooseTeam(Team team) {
        ignoreRefusal(() -> server.chooseTeam(playerId, team));
    }

    @Override
    public void chooseBrawler(BrawlerType brawler) {
        ignoreRefusal(() -> server.chooseBrawler(playerId, brawler));
    }

    @Override
    public void chooseMap(String map) {
        ignoreRefusal(() -> server.chooseMap(playerId, map));
    }

    @Override
    public void start() {
        ignoreRefusal(() -> server.requestStart(playerId));
    }

    @Override
    public RefusalReason rejection() {
        return rejection;
    }

    @Override
    public void close() {
        server.stop();
    }

    private static void ignoreRefusal(Runnable request) {
        try {
            request.run();
        } catch (LobbyException e) {
            // Same rule as over the network: a refused request is ignored
        }
    }
}
