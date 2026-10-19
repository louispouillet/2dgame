package io.github.game2d.client;

import io.github.game2d.model.Team;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.net.ServerChannel;
import io.github.game2d.protocol.RefusalReason;

public interface GameSession {
    ServerChannel channel();

    int playerId();

    void join(String name);

    void chooseTeam(Team team);

    void chooseBrawler(BrawlerType brawler);

    void chooseMap(String map);

    void start();

    RefusalReason rejection();

    void close();
}
