package io.github.game2d.protocol;

/**
 * Server answer: the player could not enter the lobby.
 *
 * @param reason why it was refused
 */
public record JoinRejected(RefusalReason reason) {}
