package io.github.game2d.model.match;

/** What the server must do after a match update. */
public enum MatchEvent {
    /** Nothing to do. */
    NONE,
    /** A new round begins: put everyone back on their spawn point. */
    ROUND_STARTED,
    /** The match is over: go back to the lobby. */
    MATCH_FINISHED
}
