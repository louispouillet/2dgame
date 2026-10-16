package io.github.game2d.protocol;

import io.github.game2d.model.brawler.BrawlerType;

public record BrawlerState(
    int id,
    BrawlerType type,
    float x,
    float y,
    float radius,
    float facing,
    int health,
    int maxHealth,
    int ammo,
    int maxAmmo,
    float reloadProgress,
    boolean moving,
    boolean attacking,
    int shotsFired,
    int lastProcessedInput
) {}
