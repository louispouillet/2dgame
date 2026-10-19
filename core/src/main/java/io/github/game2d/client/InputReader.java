package io.github.game2d.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import io.github.game2d.protocol.PlayerInput;

public class InputReader {
    private final WorldRenderer3D view;
    private final Vector2 mouse = new Vector2();
    private final InputSequence sequence;

    public InputReader(WorldRenderer3D view, InputSequence sequence) {
        this.view = view;
        this.sequence = sequence;
    }

    public PlayerInput idle() {
        return new PlayerInput(sequence.next(), 0, 0, 0, 0, false);
    }

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
