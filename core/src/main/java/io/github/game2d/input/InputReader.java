package io.github.game2d.input;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.game2d.brawler.Brawler;

public class InputReader {
    private final Viewport viewport;
    private final Vector2 mouse = new Vector2();

    public InputReader(Viewport viewport) {
        this.viewport = viewport;
    }

    public PlayerInput read(Brawler player) {
        float moveX = 0;
        float moveY = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) moveX += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) moveX -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) moveY += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) moveY -= 1;

        mouse.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouse);
        mouse.sub(player.getX(), player.getY()).nor();

        boolean shooting = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        return new PlayerInput(moveX, moveY, mouse.x, mouse.y, shooting);
    }
}
