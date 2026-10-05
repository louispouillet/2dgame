package io.github.game2d.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalChannelTest {
    @Test
    void inputIsNeutralBeforeAnythingIsSent() {
        LocalChannel channel = new LocalChannel();

        assertEquals(new PlayerInput(0, 0, 0, 0, false), channel.latestInput());
    }

    @Test
    void sentInputIsReadBackByTheServer() {
        LocalChannel channel = new LocalChannel();
        PlayerInput input = new PlayerInput(1, 0, 0, 1, true);

        channel.sendInput(input);

        assertEquals(input, channel.latestInput());
    }

    @Test
    void latestInputReplacesThePreviousOne() {
        LocalChannel channel = new LocalChannel();
        PlayerInput second = new PlayerInput(0, -1, 1, 0, false);

        channel.sendInput(new PlayerInput(1, 0, 0, 1, true));
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
        WorldSnapshot snapshot = new WorldSnapshot(List.of(), List.of());

        channel.sendSnapshot(snapshot);

        assertEquals(snapshot, channel.latestSnapshot());
    }

    @Test
    void latestSnapshotReplacesThePreviousOne() {
        LocalChannel channel = new LocalChannel();
        WorldSnapshot second = new WorldSnapshot(List.of(), List.of());

        channel.sendSnapshot(new WorldSnapshot(List.of(), List.of()));
        channel.sendSnapshot(second);

        assertSame(second, channel.latestSnapshot());
    }
}
