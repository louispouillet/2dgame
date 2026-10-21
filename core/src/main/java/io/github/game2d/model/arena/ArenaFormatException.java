package io.github.game2d.model.arena;

/** Thrown when the text of a map does not follow the map format. */
public class ArenaFormatException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    /** Creates the exception with a message that tells the line and column of the problem. */
    public ArenaFormatException(String message) {
        super(message);
    }
}
