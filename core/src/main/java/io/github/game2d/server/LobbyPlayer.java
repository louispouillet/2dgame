package io.github.game2d.server;

import io.github.game2d.model.Team;
import io.github.game2d.model.brawler.BrawlerType;

/**
 * A player waiting in the lobby. Immutable: only the {@link Lobby} replaces it in its list.
 *
 * @param id      id of the player, also the id of its brawler
 * @param name    nickname
 * @param team    chosen team
 * @param brawler chosen brawler
 */
public record LobbyPlayer(int id, String name, Team team, BrawlerType brawler) {
    /** Returns a copy in another team. */
    public LobbyPlayer withTeam(Team team) {
        return new LobbyPlayer(id, name, team, brawler);
    }

    /** Returns a copy with another brawler. */
    public LobbyPlayer withBrawler(BrawlerType brawler) {
        return new LobbyPlayer(id, name, team, brawler);
    }
}
