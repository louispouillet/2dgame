package io.github.game2d.net.kryo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.esotericsoftware.kryonet.Connection;
import io.github.game2d.protocol.PlayerInput;
import org.junit.jupiter.api.Test;

class NetworkClientChannelTest {
    private final NetworkClientChannel channel = new NetworkClientChannel(new Connection() {});

    private static PlayerInput inputAt(int sequence) {
        return new PlayerInput(sequence, 1, 0, 1, 0, false);
    }

    @Test
    void inputIsNeutralBeforeAnythingIsReceived() {
        assertEquals(0, channel.latestInput().sequence());
    }

    @Test
    void firstInputIsAccepted() {
        PlayerInput first = inputAt(1);

        channel.receive(first);

        assertSame(first, channel.latestInput());
    }

    @Test
    void newerInputReplacesTheCurrentOne() {
        PlayerInput newer = inputAt(5);

        channel.receive(inputAt(3));
        channel.receive(newer);

        assertSame(newer, channel.latestInput());
    }

    @Test
    void olderInputArrivingLateIsIgnored() {
        PlayerInput newer = inputAt(5);

        channel.receive(newer);
        channel.receive(inputAt(3));

        assertSame(newer, channel.latestInput());
    }

    @Test
    void inputWithTheSameSequenceIsIgnored() {
        PlayerInput first = inputAt(5);

        channel.receive(first);
        channel.receive(inputAt(5));

        assertSame(first, channel.latestInput());
    }
}
