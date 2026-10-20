package io.github.game2d.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import io.github.game2d.protocol.PlayerInput;

/**
 * Reads the keyboard and the mouse and turns them into {@link PlayerInput}s: {@code WASD} to move, mouse to aim, left button to shoot.
 */
public class InputReader {
    private final WorldRenderer3D view;
    private final Vector2 mouse = new Vector2();
    private final InputSequence sequence;

    /**
     * Creates a reader.
     *
     * @param view     converts the mouse position to a position in the world
     * @param sequence numbers the inputs
     */
    public InputReader(WorldRenderer3D view, InputSequence sequence) {
        this.view = view;
        this.sequence = sequence;
    }

    /** Returns a neutral input (no move, no shot), used outside of the play phase. */
    public PlayerInput idle() {
        return new PlayerInput(sequence.next(), 0, 0, 0, 0, false);
    }

    /**
     * Returns the current input.
     *
     * @param playerX horizontal position of my brawler, the aim starts from there
     * @param playerY vertical position of my brawler
     */
    public PlayerInput read(float playerX, float playerY) {
        float moveX = 0;
        float moveY = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) moveX += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) moveX -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) moveY += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) moveY -= 1;

        mouse.set(view.screenToWorld(Gdx.input.getX(), Gdx.input.getY()));
        mouse.sub(playerX, playerY).nor();

        boolean shooting = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        return new PlayerInput(sequence.next(), moveX, moveY, mouse.x, mouse.y, shooting);
    }
}
