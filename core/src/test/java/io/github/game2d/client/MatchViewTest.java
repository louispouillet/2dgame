package io.github.game2d.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.game2d.model.Team;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.model.match.MatchPhase;
import io.github.game2d.protocol.LobbyPlayerState;
import io.github.game2d.protocol.LobbyState;
import io.github.game2d.protocol.MatchState;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchViewTest {
    private static final PlayerRoster ROSTER = PlayerRoster.from(new LobbyState(
        List.of(
            new LobbyPlayerState(1, "Louis", Team.BLUE, BrawlerType.COLT),
            new LobbyPlayerState(3, "Tom", Team.BLUE, BrawlerType.SHELLY)),
        1, "maps/arena2.txt", List.of("maps/arena2.txt"), true, true), 1);

    @Test
    void nothingIsShownWhileIWatchMyself() {
        assertEquals("", MatchView.watchingText(1, ROSTER));
    }

    @Test
    void watchingAnAllyShowsTheirName() {
        assertEquals("Watching Tom", MatchView.watchingText(3, ROSTER));
    }

    @Test
    void nothingIsShownWhenNobodyIsFollowed() {
        assertEquals("", MatchView.watchingText(WorldSnapshot.NOBODY, ROSTER));
    }

    @Test
    void unknownAllyIsStillAnnounced() {
        assertEquals("Watching an ally", MatchView.watchingText(9, ROSTER));
    }

    private static MatchState match(MatchPhase phase, float timeLeft, Team roundWinner, Team matchWinner) {
        return new MatchState(phase, 2, 1, 0, timeLeft, roundWinner, matchWinner);
    }

    @Test
    void withoutMatchStateThereIsNothingToShow() {
        assertFalse(new MatchView(null, Team.BLUE).hasMatch());
    }

    @Test
    void myScoreIsAlwaysOnTheLeft() {
        MatchState state = match(MatchPhase.PLAYING, 0f, null, null);

        MatchView blue = new MatchView(state, Team.BLUE);
        MatchView red = new MatchView(state, Team.RED);

        assertEquals(1, blue.myScore());
        assertEquals(0, blue.enemyScore());
        assertEquals(0, red.myScore());
        assertEquals(1, red.enemyScore());
    }

    @Test
    void showsTheRoundNumber() {
        assertEquals("ROUND 2", new MatchView(match(MatchPhase.PLAYING, 0f, null, null), Team.BLUE).roundLabel());
    }

    @Test
    void countdownShowsTheSecondsLeftRoundedUp() {
        assertEquals("3", centerText(MatchPhase.COUNTDOWN, 2.4f, null, null, Team.BLUE));
        assertEquals("1", centerText(MatchPhase.COUNTDOWN, 0.1f, null, null, Team.BLUE));
        assertEquals("", centerText(MatchPhase.COUNTDOWN, 0f, null, null, Team.BLUE));
    }

    @Test
    void nothingIsShownWhilePlaying() {
        assertEquals("", centerText(MatchPhase.PLAYING, 0f, null, null, Team.BLUE));
    }

    @Test
    void roundResultDependsOnWhoIsLooking() {
        assertEquals("Round won!", centerText(MatchPhase.ROUND_OVER, 2f, Team.RED, null, Team.RED));
        assertEquals("Round lost", centerText(MatchPhase.ROUND_OVER, 2f, Team.RED, null, Team.BLUE));
        assertEquals("Draw, replaying the round", centerText(MatchPhase.ROUND_OVER, 2f, null, null, Team.BLUE));
    }

    @Test
    void matchResultDependsOnWhoIsLooking() {
        assertEquals("VICTORY", centerText(MatchPhase.MATCH_OVER, 4f, Team.BLUE, Team.BLUE, Team.BLUE));
        assertEquals("DEFEAT", centerText(MatchPhase.MATCH_OVER, 4f, Team.BLUE, Team.BLUE, Team.RED));
    }

    @Test
    void toneTellsGoodAndBadNews() {
        assertEquals(MatchView.Tone.WIN, tone(MatchPhase.MATCH_OVER, Team.BLUE, Team.BLUE));
        assertEquals(MatchView.Tone.LOSS, tone(MatchPhase.ROUND_OVER, Team.RED, Team.BLUE));
        assertEquals(MatchView.Tone.NEUTRAL, tone(MatchPhase.ROUND_OVER, null, Team.BLUE));
        assertEquals(MatchView.Tone.NEUTRAL, tone(MatchPhase.COUNTDOWN, null, Team.BLUE));
    }

    private static String centerText(MatchPhase phase, float timeLeft, Team roundWinner, Team matchWinner, Team me) {
        return new MatchView(match(phase, timeLeft, roundWinner, matchWinner), me).centerText();
    }

    private static MatchView.Tone tone(MatchPhase phase, Team winner, Team me) {
        return new MatchView(match(phase, 1f, winner, phase == MatchPhase.MATCH_OVER ? winner : null), me).centerTone();
    }
}
