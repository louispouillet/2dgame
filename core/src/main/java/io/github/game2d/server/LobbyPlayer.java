package io.github.game2d.server;

import io.github.game2d.model.Team;
import io.github.game2d.model.brawler.BrawlerType;

public record LobbyPlayer(int id, String name, Team team, BrawlerType brawler) {
    public LobbyPlayer withTeam(Team team) {
        return new LobbyPlayer(id, name, team, brawler);
    }

    public LobbyPlayer withBrawler(BrawlerType brawler) {
        return new LobbyPlayer(id, name, team, brawler);
    }
}
