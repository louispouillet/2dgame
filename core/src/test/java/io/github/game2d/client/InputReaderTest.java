package io.github.game2d.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InputReaderTest {
    @Test
    void sequenceKeepsGrowingWhenANewReaderTakesOver() {
        // A new game screen builds a new InputReader, but the server keeps the highest sequence
        // of the connection and drops every input numbered below it.
        InputSequence sequence = new InputSequence();
        InputReader firstGame = new InputReader(null, sequence);
        int last = 0;
        for (int i = 0; i < 100; i++) {
            last = firstGame.idle().sequence();
        }

        InputReader secondGame = new InputReader(null, sequence);

        assertTrue(secondGame.idle().sequence() > last);
    }

    @Test
    void sequenceStartsAtOne() {
        assertEquals(1, new InputReader(null, new InputSequence()).idle().sequence());
    }
}
