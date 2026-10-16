package io.github.game2d.protocol;

import io.github.game2d.model.brawler.BrawlerType;

public record ProjectileState (
    int id,
    float x,
    float y,
    float radius,
    int ownerId,
    float dirX,
    float dirY,
    BrawlerType shooter
) {}
