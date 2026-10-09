package io.github.game2d.server;

import io.github.game2d.protocol.RefusalReason;

public class LobbyException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    private final RefusalReason reason;

    public LobbyException(RefusalReason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public RefusalReason getReason() {
        return reason;
    }
}
