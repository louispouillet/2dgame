package io.github.game2d.client;

import io.github.game2d.model.Team;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.net.ServerChannel;
import io.github.game2d.protocol.RefusalReason;

/**
 * The connection of the client to a game, either local ({@link LocalSession}) or over the network ({@link RemoteSession}).
 * It carries the lobby commands and gives the channel used during the match.
 */
public interface GameSession {
    /** Returns the channel to send inputs and read snapshots and lobby states. */
    ServerChannel channel();

    /** Returns my player id (also my brawler id), or -1 if I have not joined. */
    int playerId();

    /** Asks to enter the lobby with this nickname. */
    void join(String name);

    /** Asks to join a team. */
    void chooseTeam(Team team);

    /** Asks to play a brawler. */
    void chooseBrawler(BrawlerType brawler);

    /** Asks to play on a map (leader only). */
    void chooseMap(String map);

    /** Asks to start the match (leader only). */
    void start();

    /** Returns why the join was refused, or {@code null}. */
    RefusalReason rejection();

    /** Ends the session and frees its resources. */
    void close();
}
