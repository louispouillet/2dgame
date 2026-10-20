package io.github.game2d.protocol;

/**
 * Client request (leader only): play on this map.
 *
 * @param map path of the map, as listed in {@link LobbyState#maps()}
 */
public record ChooseMap(String map) {}
