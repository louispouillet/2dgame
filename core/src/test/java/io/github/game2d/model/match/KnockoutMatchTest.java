package io.github.game2d.model.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.game2d.model.Team;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;

class KnockoutMatchTest {
    private static final ToIntFunction<Team> BOTH_ALIVE = team -> 1;
    private static final ToIntFunction<Team> RED_OUT = team -> team == Team.RED ? 0 : 1;
    private static final ToIntFunction<Team> BLUE_OUT = team -> team == Team.BLUE ? 0 : 1;
    private static final ToIntFunction<Team> BOTH_OUT = team -> 0;

    private final KnockoutMatch match = new KnockoutMatch();

    private void play() {
        match.update(KnockoutMatch.COUNTDOWN_TIME, BOTH_ALIVE);
    }

    private MatchEvent winRound(ToIntFunction<Team> alive) {
        play();
        match.update(0f, alive);
        return match.update(KnockoutMatch.ROUND_OVER_TIME, BOTH_ALIVE);
    }

    @Test
    void startsWithTheCountdownOfRoundOne() {
        assertEquals(MatchPhase.COUNTDOWN, match.phase());
        assertEquals(1, match.round());
        assertEquals(KnockoutMatch.COUNTDOWN_TIME, match.timeLeft());
        assertEquals(0, match.score(Team.BLUE));
    }

    @Test
    void roundStartsWhenTheCountdownEnds() {
        match.update(KnockoutMatch.COUNTDOWN_TIME - 0.5f, BOTH_ALIVE);
        assertEquals(MatchPhase.COUNTDOWN, match.phase());

        match.update(0.5f, BOTH_ALIVE);

        assertEquals(MatchPhase.PLAYING, match.phase());
    }

    @Test
    void roundGoesOnWhileBothTeamsHavePlayersAlive() {
        play();

        match.update(10f, BOTH_ALIVE);

        assertEquals(MatchPhase.PLAYING, match.phase());
    }

    @Test
    void teamWinsTheRoundWhenTheEnemyTeamIsEliminated() {
        play();

        match.update(0f, RED_OUT);

        assertEquals(MatchPhase.ROUND_OVER, match.phase());
        assertEquals(Team.BLUE, match.roundWinner());
        assertEquals(1, match.score(Team.BLUE));
        assertEquals(0, match.score(Team.RED));
    }

    @Test
    void nextRoundStartsAfterThePause() {
        play();
        match.update(0f, BLUE_OUT);

        MatchEvent event = match.update(KnockoutMatch.ROUND_OVER_TIME, BOTH_ALIVE);

        assertEquals(MatchEvent.ROUND_STARTED, event);
        assertEquals(MatchPhase.COUNTDOWN, match.phase());
        assertEquals(2, match.round());
        assertNull(match.roundWinner());
    }

    @Test
    void drawReplaysTheSameRoundWithoutScoring() {
        play();
        match.update(0f, BOTH_OUT);

        assertEquals(MatchPhase.ROUND_OVER, match.phase());
        assertNull(match.roundWinner());
        assertEquals(0, match.score(Team.BLUE));
        assertEquals(0, match.score(Team.RED));

        match.update(KnockoutMatch.ROUND_OVER_TIME, BOTH_ALIVE);

        assertEquals(1, match.round());
    }

    @Test
    void firstTeamToWinTwoRoundsWinsTheMatch() {
        winRound(RED_OUT);
        play();

        match.update(0f, RED_OUT);

        assertEquals(MatchPhase.MATCH_OVER, match.phase());
        assertEquals(Team.BLUE, match.matchWinner());
        assertEquals(2, match.score(Team.BLUE));
    }

    @Test
    void matchCanGoToAThirdRound() {
        winRound(RED_OUT);
        winRound(BLUE_OUT);
        play();

        match.update(0f, BLUE_OUT);

        assertEquals(3, match.round());
        assertEquals(Team.RED, match.matchWinner());
    }

    @Test
    void matchFinishesOnceAfterTheEndScreen() {
        winRound(RED_OUT);
        play();
        match.update(0f, RED_OUT);

        assertEquals(MatchEvent.NONE, match.update(KnockoutMatch.MATCH_OVER_TIME - 1f, BOTH_ALIVE));
        assertEquals(MatchEvent.MATCH_FINISHED, match.update(1f, BOTH_ALIVE));
        assertEquals(MatchEvent.NONE, match.update(1f, BOTH_ALIVE));
    }

    @Test
    void teamThatLeavesForfeitsTheMatch() {
        play();

        match.forfeit(Team.RED);

        assertEquals(MatchPhase.MATCH_OVER, match.phase());
        assertEquals(Team.BLUE, match.matchWinner());
    }
}
