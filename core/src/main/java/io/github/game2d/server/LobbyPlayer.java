package io.github.game2d.server;

import io.github.game2d.model.Team;

public record LobbyPlayer(int id, String name, Team team) {
}
