package io.github.game2d.server;

import io.github.game2d.protocol.RefusalReason;

/** Thrown when the lobby refuses a request; it carries the reason. */
public class LobbyException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    private final RefusalReason reason;

    /** Creates the exception for a reason. */
    public LobbyException(RefusalReason reason) {
        super(reason.name());
        this.reason = reason;
    }

    /** Returns why the request was refused. */
    public RefusalReason getReason() {
        return reason;
    }
}
