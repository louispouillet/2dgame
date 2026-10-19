package io.github.game2d.client;

import io.github.game2d.model.Team;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.net.ServerChannel;
import io.github.game2d.net.kryo.NetworkClient;
import io.github.game2d.net.kryo.NetworkRegistry;
import io.github.game2d.protocol.RefusalReason;
import java.io.IOException;

public class RemoteSession implements GameSession {
    private final NetworkClient client = new NetworkClient();

    public RemoteSession(String host) throws IOException {
        client.connect(host, NetworkRegistry.DEFAULT_TCP_PORT, NetworkRegistry.DEFAULT_UDP_PORT);
    }

    @Override
    public ServerChannel channel() {
        return client.channel();
    }

    @Override
    public int playerId() {
        return client.channel().getBrawlerId();
    }

    @Override
    public void join(String name) {
        client.channel().sendJoin(name);
    }

    @Override
    public void chooseTeam(Team team) {
        client.channel().sendChooseTeam(team);
    }

    @Override
    public void chooseBrawler(BrawlerType brawler) {
        client.channel().sendChooseBrawler(brawler);
    }

    @Override
    public void chooseMap(String map) {
        client.channel().sendChooseMap(map);
    }

    @Override
    public void start() {
        client.channel().sendStart();
    }

    @Override
    public RefusalReason rejection() {
        return client.channel().rejection();
    }

    @Override
    public void close() {
        client.stop();
    }
}
