package io.github.game2d.client;

/**
 * Numbers the inputs of one session. The server drops any input numbered below the highest it has received
 * on a connection, so the count must go on across games instead of restarting with each game screen.
 */
public class InputSequence {
    private int last;

    public int next() {
        return ++last;
    }
}
