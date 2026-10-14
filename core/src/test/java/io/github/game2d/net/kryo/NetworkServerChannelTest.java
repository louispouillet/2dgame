package io.github.game2d.net.kryo;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.esotericsoftware.kryonet.Client;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class NetworkServerChannelTest {
    private final NetworkServerChannel channel = new NetworkServerChannel(new Client());

    private static WorldSnapshot snapshotAt(int tick) {
        return new WorldSnapshot(tick, List.of(), List.of(), null);
    }

    @Test
    void noSnapshotBeforeAnythingIsReceived() {
        assertNull(channel.latestSnapshot());
    }

    @Test
    void firstSnapshotIsAccepted() {
        WorldSnapshot first = snapshotAt(1);

        channel.receive(first);

        assertSame(first, channel.latestSnapshot());
    }

    @Test
    void newerSnapshotReplacesTheCurrentOne() {
        WorldSnapshot newer = snapshotAt(10);

        channel.receive(snapshotAt(5));
        channel.receive(newer);

        assertSame(newer, channel.latestSnapshot());
    }

    @Test
    void olderSnapshotArrivingLateIsIgnored() {
        WorldSnapshot newer = snapshotAt(10);

        channel.receive(newer);
        channel.receive(snapshotAt(8));

        assertSame(newer, channel.latestSnapshot());
    }

    @Test
    void snapshotWithTheSameTickIsIgnored() {
        WorldSnapshot first = snapshotAt(10);

        channel.receive(first);
        channel.receive(snapshotAt(10));

        assertSame(first, channel.latestSnapshot());
    }
}
