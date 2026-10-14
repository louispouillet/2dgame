package io.github.game2d.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.game2d.protocol.LobbyState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalChannelTest {
    @Test
    void inputIsNeutralBeforeAnythingIsSent() {
        LocalChannel channel = new LocalChannel();

        assertEquals(new PlayerInput(0, 0, 0, 0, 0, false), channel.latestInput());
    }

    @Test
    void sentInputIsReadBackByTheServer() {
        LocalChannel channel = new LocalChannel();
        PlayerInput input = new PlayerInput(1, 1, 0, 0, 1, true);

        channel.sendInput(input);

        assertEquals(input, channel.latestInput());
    }

    @Test
    void latestInputReplacesThePreviousOne() {
        LocalChannel channel = new LocalChannel();
        PlayerInput second = new PlayerInput(2, 0, -1, 1, 0, false);

        channel.sendInput(new PlayerInput(1, 1, 0, 0, 1, true));
        channel.sendInput(second);

        assertEquals(second, channel.latestInput());
    }

    @Test
    void snapshotIsAbsentBeforeAnythingIsSent() {
        LocalChannel channel = new LocalChannel();

        assertNull(channel.latestSnapshot());
    }

    @Test
    void sentSnapshotIsReadBackByTheClient() {
        LocalChannel channel = new LocalChannel();
        WorldSnapshot snapshot = new WorldSnapshot(0, List.of(), List.of(), null);

        channel.sendSnapshot(snapshot);

        assertEquals(snapshot, channel.latestSnapshot());
    }

    @Test
    void latestSnapshotReplacesThePreviousOne() {
        LocalChannel channel = new LocalChannel();
        WorldSnapshot second = new WorldSnapshot(2, List.of(), List.of(), null);

        channel.sendSnapshot(new WorldSnapshot(1, List.of(), List.of(), null));
        channel.sendSnapshot(second);

        assertSame(second, channel.latestSnapshot());
    }

    @Test
    void noLobbyBeforeTheServerSendsOne() {
        assertNull(new LocalChannel().latestLobby());
    }

    @Test
    void sentLobbyIsReadBackByTheClient() {
        LocalChannel channel = new LocalChannel();
        LobbyState lobby = new LobbyState(List.of(), 1, "maps/arena2.txt", List.of("maps/arena2.txt"), false, false);

        channel.sendLobby(lobby);

        assertSame(lobby, channel.latestLobby());
    }
}
