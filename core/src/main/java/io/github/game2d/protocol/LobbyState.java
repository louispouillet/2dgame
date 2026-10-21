package io.github.game2d.protocol;

import java.util.List;

/**
 * The state of the lobby, sent to everybody after each change.
 *
 * @param players  the players, in order of arrival
 * @param leaderId id of the leader, the first player
 * @param map      path of the chosen map
 * @param maps     paths of the available maps
 * @param canStart whether both teams have between 1 and 3 players
 * @param started  whether a match is being played
 */
public record LobbyState(
    List<LobbyPlayerState> players,
    int leaderId,
    String map,
    List<String> maps,
    boolean canStart,
    boolean started
) {}
