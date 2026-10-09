package io.github.game2d.protocol;

import io.github.game2d.model.brawler.BrawlerType;

public record BrawlerState(
    int id,
    float x,
    float y,
    float radius,
    int health,
    int maxHealth,
    int ammo,
    int maxAmmo,
    float reloadProgress,
    BrawlerType type,
    int lastProcessedInput
) {}
