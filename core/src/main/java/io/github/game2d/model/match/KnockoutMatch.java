package io.github.game2d.model.match;

import io.github.game2d.model.Team;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * The rounds of a Knockout match: countdown, play, round over, until a team wins {@value #ROUNDS_TO_WIN} rounds.
 * Pure state machine: the caller gives the elapsed time and the number of living brawlers.
 */
public final class KnockoutMatch {
    /** Rounds a team must win to win the match. */
    public static final int ROUNDS_TO_WIN = 2;
    /** Seconds of countdown before each round, nobody can move. */
    public static final float COUNTDOWN_TIME = 3f;
    /** Seconds shown after a round, before the next countdown. */
    public static final float ROUND_OVER_TIME = 3f;
    /** Seconds shown after the match, before going back to the lobby. */
    public static final float MATCH_OVER_TIME = 5f;

    private final Map<Team, Integer> scores = new EnumMap<>(Team.class);
    private MatchPhase phase = MatchPhase.COUNTDOWN;
    private float timeLeft = COUNTDOWN_TIME;
    private int round = 1;
    private Team roundWinner;
    private Team matchWinner;

    /** Starts a match at round 1, in the countdown. */
    public KnockoutMatch() {
        for (Team team : Team.values()) {
            scores.put(team, 0);
        }
    }

    /**
     * Advances the match.
     *
     * @param dt         elapsed time, in seconds
     * @param aliveCount gives the number of living brawlers of a team
     * @return what the server has to do now, or {@link MatchEvent#NONE}
     */
    public MatchEvent update(float dt, ToIntFunction<Team> aliveCount) {
        return switch (phase) {
            case COUNTDOWN -> countDown(dt);
            case PLAYING -> checkRoundEnd(aliveCount);
            case ROUND_OVER -> waitForNextRound(dt);
            case MATCH_OVER -> waitForTheEnd(dt);
        };
    }

    /**
     * Ends the match now: the other team wins. Does nothing when the match is already over.
     *
     * @param team the team that gives up
     */
    public void forfeit(Team team) {
        if (phase == MatchPhase.MATCH_OVER) {
            return;
        }
        matchWinner = opponentOf(team);
        startPhase(MatchPhase.MATCH_OVER, MATCH_OVER_TIME);
    }

    /** Returns the current phase. */
    public MatchPhase phase() {
        return phase;
    }

    /** Returns the current round number, starting at 1. */
    public int round() {
        return round;
    }

    /** Returns the rounds won by a team. */
    public int score(Team team) {
        return scores.get(team);
    }

    /** Returns the seconds left in the current timed phase. */
    public float timeLeft() {
        return timeLeft;
    }

    /** Returns the winner of the last round, or {@code null} (no round over yet, or a draw). */
    public Team roundWinner() {
        return roundWinner;
    }

    /** Returns the winner of the match, or {@code null} while it is not decided. */
    public Team matchWinner() {
        return matchWinner;
    }

    private MatchEvent countDown(float dt) {
        if (tick(dt)) {
            startPhase(MatchPhase.PLAYING, 0f);
        }
        return MatchEvent.NONE;
    }

    private MatchEvent checkRoundEnd(ToIntFunction<Team> aliveCount) {
        boolean blueOut = aliveCount.applyAsInt(Team.BLUE) == 0;
        boolean redOut = aliveCount.applyAsInt(Team.RED) == 0;
        if (!blueOut && !redOut) {
            return MatchEvent.NONE;
        }
        roundWinner = blueOut && redOut ? null : (blueOut ? Team.RED : Team.BLUE);
        if (roundWinner != null) {
            scores.merge(roundWinner, 1, Integer::sum);
        }
        if (roundWinner != null && scores.get(roundWinner) >= ROUNDS_TO_WIN) {
            matchWinner = roundWinner;
            startPhase(MatchPhase.MATCH_OVER, MATCH_OVER_TIME);
        } else {
            startPhase(MatchPhase.ROUND_OVER, ROUND_OVER_TIME);
        }
        return MatchEvent.NONE;
    }

    private MatchEvent waitForNextRound(float dt) {
        if (!tick(dt)) {
            return MatchEvent.NONE;
        }
        if (roundWinner != null) {
            round++;
        }
        roundWinner = null;
        startPhase(MatchPhase.COUNTDOWN, COUNTDOWN_TIME);
        return MatchEvent.ROUND_STARTED;
    }

    private MatchEvent waitForTheEnd(float dt) {
        if (timeLeft <= 0f || !tick(dt)) {
            return MatchEvent.NONE;
        }
        return MatchEvent.MATCH_FINISHED;
    }

    private boolean tick(float dt) {
        timeLeft = Math.max(0f, timeLeft - dt);
        return timeLeft == 0f;
    }

    private void startPhase(MatchPhase next, float duration) {
        phase = next;
        timeLeft = duration;
    }

    private static Team opponentOf(Team team) {
        return team == Team.BLUE ? Team.RED : Team.BLUE;
    }
}
