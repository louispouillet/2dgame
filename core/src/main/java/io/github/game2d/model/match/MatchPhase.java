package io.github.game2d.model.match;

/** The phases of a Knockout match. */
public enum MatchPhase {
    /** Before a round: nobody moves. */
    COUNTDOWN,
    /** The round is played. */
    PLAYING,
    /** A round just ended. */
    ROUND_OVER,
    /** A team won the match. */
    MATCH_OVER
}
