package io.github.game2d.protocol;

/**
 * What a player does during one input step, sent by the client.
 *
 * @param sequence number of the input, growing; the server confirms it in {@link BrawlerState#lastProcessedInput()}
 * @param moveX    horizontal movement direction
 * @param moveY    vertical movement direction
 * @param aimX     horizontal aim direction
 * @param aimY     vertical aim direction
 * @param shooting whether the player is firing
 */
public record PlayerInput(
    int sequence,
    float moveX,
    float moveY,
    float aimX,
    float aimY,
    boolean shooting
) {}
