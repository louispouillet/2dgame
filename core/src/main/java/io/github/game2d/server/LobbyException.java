package io.github.game2d.server;

public class LobbyException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public enum Reason {
        INVALID_NAME,
        NAME_TAKEN,
        LOBBY_FULL,
        TEAM_FULL,
        NOT_LEADER,
        UNKNOWN_MAP
    }

    private final Reason reason;

    public LobbyException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
