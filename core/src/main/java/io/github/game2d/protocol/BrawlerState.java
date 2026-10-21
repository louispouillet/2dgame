package io.github.game2d.protocol;

import io.github.game2d.model.brawler.BrawlerType;

/**
 * What a client knows about one brawler at one tick.
 *
 * @param id                 id of the brawler (also the id of its player)
 * @param type               kind of brawler
 * @param x                  horizontal position, in tiles
 * @param y                  vertical position, in tiles
 * @param radius             radius of the hitbox, in tiles
 * @param facing             direction it looks at, in radians
 * @param health             current health points
 * @param maxHealth          health points at full health
 * @param ammo               ammo left
 * @param maxAmmo            ammo capacity
 * @param reloadProgress     progress of the reload, from 0 to 1
 * @param moving             whether it is walking
 * @param attacking          whether bullets of its attack are still leaving
 * @param shotsFired         number of shots since the start of the match
 * @param lastProcessedInput sequence number of the last input the server applied (for the prediction)
 */
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
