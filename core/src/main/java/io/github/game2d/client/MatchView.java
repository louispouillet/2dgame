package io.github.game2d.client;

import io.github.game2d.model.Team;
import io.github.game2d.protocol.MatchState;

public final class MatchView {
    public enum Tone {
        NEUTRAL,
        WIN,
        LOSS
    }

    private final MatchState match;
    private final Team myTeam;

    public MatchView(MatchState match, Team myTeam) {
        this.match = match;
        this.myTeam = myTeam == null ? Team.BLUE : myTeam;
    }

    public boolean hasMatch() {
        return match != null;
    }

    public int myScore() {
        return myTeam == Team.BLUE ? match.blueScore() : match.redScore();
    }

    public int enemyScore() {
        return myTeam == Team.BLUE ? match.redScore() : match.blueScore();
    }

    public String roundLabel() {
        return "ROUND " + match.round();
    }

    public String centerText() {
        return switch (match.phase()) {
            case COUNTDOWN -> countdown();
            case PLAYING -> "";
            case ROUND_OVER -> match.roundWinner() == null ? "Draw, replaying the round"
                : (match.roundWinner() == myTeam ? "Round won!" : "Round lost");
            case MATCH_OVER -> match.matchWinner() == myTeam ? "VICTORY" : "DEFEAT";
        };
    }

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

    private String countdown() {
        int seconds = (int) Math.ceil(match.timeLeft());
        return seconds > 0 ? String.valueOf(seconds) : "";
    }
}
