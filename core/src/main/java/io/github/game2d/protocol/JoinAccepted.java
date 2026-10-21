package io.github.game2d.protocol;

/**
 * Server answer: the player entered the lobby.
 *
 * @param brawlerId id of the player, which is also the id of its brawler
 */
public record JoinAccepted(int brawlerId) {}
