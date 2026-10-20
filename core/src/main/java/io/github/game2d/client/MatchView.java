package io.github.game2d.client;

import io.github.game2d.model.Team;
import io.github.game2d.protocol.MatchState;
import io.github.game2d.protocol.WorldSnapshot;

/**
 * What the match HUD shows, decided from a {@link MatchState} and my team. Pure Java, no libGDX.
 * My score is always on the left.
 */
public final class MatchView {
    /** Colour intent of the center text. */
    public enum Tone {
        /** Nothing won or lost: countdown, draw. */
        NEUTRAL,
        /** My team won. */
        WIN,
        /** My team lost. */
        LOSS
    }

    private final MatchState match;
    private final Team myTeam;

    /**
     * Creates a view.
     *
     * @param match  the match state of the snapshot
     * @param myTeam my team; blue if unknown
     */
    public MatchView(MatchState match, Team myTeam) {
        this.match = match;
        this.myTeam = myTeam == null ? Team.BLUE : myTeam;
    }

    /** Whether there is a match to show. */
    public boolean hasMatch() {
        return match != null;
    }

    /** Returns the rounds won by my team. */
    public int myScore() {
        return myTeam == Team.BLUE ? match.blueScore() : match.redScore();
    }

    /** Returns the rounds won by the other team. */
    public int enemyScore() {
        return myTeam == Team.BLUE ? match.redScore() : match.blueScore();
    }

    /** Returns the title of the round, for example {@code ROUND 2}. */
    public String roundLabel() {
        return "ROUND " + match.round();
    }

    /**
     * Returns the big text at the center of the screen: countdown, round result or match result. Empty while playing.
     */
    public String centerText() {
        return switch (match.phase()) {
            case COUNTDOWN -> countdown();
            case PLAYING -> "";
            case ROUND_OVER -> match.roundWinner() == null ? "Draw, replaying the round"
                : (match.roundWinner() == myTeam ? "Round won!" : "Round lost");
            case MATCH_OVER -> match.matchWinner() == myTeam ? "VICTORY" : "DEFEAT";
        };
    }

    /** Returns the tone of the center text. */
    public Tone centerTone() {
        Team winner = switch (match.phase()) {
            case ROUND_OVER -> match.roundWinner();
            case MATCH_OVER -> match.matchWinner();
            default -> null;
        };
        if (winner == null) {
            return Tone.NEUTRAL;
        }
        return winner == myTeam ? Tone.WIN : Tone.LOSS;
    }

    /** "Watching Tom" once I am dead and the camera follows an ally; empty while I watch myself or nobody. */
    public static String watchingText(int followedId, PlayerRoster roster) {
        if (followedId == WorldSnapshot.NOBODY || roster.isMe(followedId)) {
            return "";
        }
        String name = roster.nameOf(followedId);
        return "Watching " + (name.isEmpty() ? "an ally" : name);
    }

    /** Returns the remaining seconds, rounded up, or an empty text at zero. */
    private String countdown() {
        int seconds = (int) Math.ceil(match.timeLeft());
        return seconds > 0 ? String.valueOf(seconds) : "";
    }
}
