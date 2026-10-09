package io.github.game2d.protocol;

import io.github.game2d.model.Team;

public record LobbyPlayerState(int id, String name, Team team) {}
