package io.github.game2d.protocol;

import io.github.game2d.model.Team;
import io.github.game2d.model.match.MatchPhase;

/**
 * Where the Knockout match stands, copied into every snapshot.
 *
 * @param phase       current phase
 * @param round       current round number, from 1
 * @param blueScore   rounds won by the blue team
 * @param redScore    rounds won by the red team
 * @param timeLeft    seconds left in the current timed phase
 * @param roundWinner winner of the last round, {@code null} if none or a draw
 * @param matchWinner winner of the match, {@code null} while undecided
 */
public record MatchState(
    MatchPhase phase,
    int round,
    int blueScore,
    int redScore,
    float timeLeft,
    Team roundWinner,
    Team matchWinner
) {}
