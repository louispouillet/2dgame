package io.github.game2d.client;

import io.github.game2d.protocol.BrawlerState;

/**
 * Where to draw the HUD of a brawler: a point under its feet on the screen.
 *
 * @param brawler the brawler
 * @param centerX horizontal center on the screen, in pixels
 * @param bottomY vertical position of the bottom, in pixels
 * @param unit    size in pixels of one tile at that place, to scale the HUD
 */
public record HudAnchor(BrawlerState brawler, float centerX, float bottomY, float unit) {
}
