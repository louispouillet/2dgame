package io.github.game2d;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import io.github.game2d.brawler.Colt;
import io.github.game2d.brawler.Shelly;
import io.github.game2d.input.InputReader;
import io.github.game2d.input.PlayerInput;
import io.github.game2d.render.WorldRenderer;
import io.github.game2d.world.GameWorld;

public class Main extends ApplicationAdapter {
    private GameWorld world;
    private WorldRenderer renderer;
    private InputReader inputReader;
    private Shelly colt;

    @Override
    public void create() {
        world = new GameWorld();
        renderer = new WorldRenderer();
        inputReader = new InputReader(renderer.getViewport());
        colt = new Shelly(8, 4.5f);
        world.add(colt);
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
    }

    @Override
    public void render() {
        PlayerInput input = inputReader.read(colt);
        world.applyInput(colt, input);
        world.update(Gdx.graphics.getDeltaTime());
        renderer.render(world);
    }

    @Override
    public void dispose() {
        renderer.dispose();
    }
}
