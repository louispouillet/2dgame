package io.github.game2d.protocol;

import java.util.List;

public record LobbyState(
    List<LobbyPlayerState> players,
    int leaderId,
    String map,
    List<String> maps,
    boolean canStart,
    boolean started
) {}
