package io.github.game2d.client;

import io.github.game2d.protocol.RefusalReason;

public final class RefusalMessages {
    private RefusalMessages() {
    }

    public static String describe(RefusalReason reason) {
        return switch (reason) {
            case INVALID_NAME -> "Invalid nickname: 1 to 16 letters, digits, _ or -";
            case NAME_TAKEN -> "This nickname is already taken";
            case LOBBY_FULL -> "The lobby is full";
            case TEAM_FULL -> "This team is full";
            case NOT_LEADER -> "Only the leader can do that";
            case UNKNOWN_MAP -> "Unknown map";
            case TEAMS_NOT_READY -> "Each team needs at least one player";
            case GAME_IN_PROGRESS -> "A game is in progress";
        };
    }
}
