package io.github.game2d.protocol;

import io.github.game2d.model.brawler.BrawlerType;

/** The shooter's type is null when the shooter is gone: the bullet is then drawn with the default look. */
public record ProjectileState (
    int id,
    int ownerId,
    BrawlerType ownerType,
    float x,
    float y,
    float dirX,
    float dirY,
    float radius
) {}
