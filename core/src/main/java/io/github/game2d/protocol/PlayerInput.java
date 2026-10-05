package io.github.game2d.protocol;

public record PlayerInput(
    float moveX,
    float moveY,
    float aimX,
    float aimY,
    boolean shooting
) {}
