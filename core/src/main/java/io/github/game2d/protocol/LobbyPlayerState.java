package io.github.game2d.protocol;

import io.github.game2d.model.Team;
import io.github.game2d.model.brawler.BrawlerType;

/**
 * A player of the lobby, as sent to the clients.
 *
 * @param id      id of the player
 * @param name    nickname
 * @param team    chosen team
 * @param brawler chosen brawler
 */
public record LobbyPlayerState(int id, String name, Team team, BrawlerType brawler) {}
