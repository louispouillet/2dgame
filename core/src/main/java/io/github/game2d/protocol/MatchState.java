package io.github.game2d.protocol;

import io.github.game2d.model.Team;
import io.github.game2d.model.match.MatchPhase;

public record MatchState(
    MatchPhase phase,
    int round,
    int blueScore,
    int redScore,
    float timeLeft,
    Team roundWinner,
    Team matchWinner
) {}
