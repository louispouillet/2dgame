package io.github.game2d.protocol;

public record BrawlerState(
    int id,
    float x,
    float y,
    float radius,
    int health,
    int maxHealth,
    int ammo,
    int maxAmmo,
    float reloadProgress
) {}
