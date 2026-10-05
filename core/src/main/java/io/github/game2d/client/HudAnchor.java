package io.github.game2d.client;

import io.github.game2d.protocol.BrawlerState;

public record HudAnchor(BrawlerState brawler, float centerX, float bottomY, float unit) {
}
