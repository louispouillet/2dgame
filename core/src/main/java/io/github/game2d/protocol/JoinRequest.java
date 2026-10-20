package io.github.game2d.protocol;

/**
 * Client request: enter the lobby. It must be the first message of a client.
 *
 * @param name nickname asked by the player
 */
public record JoinRequest(String name) {}
