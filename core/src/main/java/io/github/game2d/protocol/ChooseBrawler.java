package io.github.game2d.protocol;

import io.github.game2d.model.brawler.BrawlerType;

/**
 * Client request: play this brawler.
 *
 * @param brawler the chosen kind of brawler
 */
public record ChooseBrawler(BrawlerType brawler) {}
