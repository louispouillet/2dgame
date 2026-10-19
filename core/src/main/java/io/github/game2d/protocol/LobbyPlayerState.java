package io.github.game2d.protocol;

import io.github.game2d.model.Team;
import io.github.game2d.model.brawler.BrawlerType;

public record LobbyPlayerState(int id, String name, Team team, BrawlerType brawler) {}
