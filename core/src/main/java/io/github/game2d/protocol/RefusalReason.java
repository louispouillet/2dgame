package io.github.game2d.protocol;

/** Why the lobby refused a request. */
public enum RefusalReason {
    /** The nickname is empty, too long or has forbidden characters. */
    INVALID_NAME,
    /** Another player already uses this nickname. */
    NAME_TAKEN,
    /** There are already 6 players. */
    LOBBY_FULL,
    /** The team already has 3 players. */
    TEAM_FULL,
    /** Only the leader can do this. */
    NOT_LEADER,
    /** The map is not in the list. */
    UNKNOWN_MAP,
    /** The brawler does not exist. */
    UNKNOWN_BRAWLER,
    /** A team is empty or too big, the match cannot start. */
    TEAMS_NOT_READY,
    /** A match is being played: the lobby is locked. */
    GAME_IN_PROGRESS
}
