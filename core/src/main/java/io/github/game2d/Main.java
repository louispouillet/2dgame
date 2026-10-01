package io.github.game2d;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.game2d.brawler.Colt;
import io.github.game2d.model.GameObject;
import io.github.game2d.world.GameWorld;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private GameWorld world;
    private ShapeRenderer shapes;
    private FitViewport viewport;
    private Colt colt;

    @Override
    public void create() {
        viewport = new FitViewport(16, 9);   // l'arène fait 16 x 9 unités
        shapes = new ShapeRenderer();
        world = new GameWorld();
        colt = new Colt(8, 4.5f);
        world.add(colt);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    private void readInput() {
        float dx = 0;
        float dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1;
        colt.setDirection(dx, dy);
    }

    @Override
    public void render() {
        readInput();
        world.update(Gdx.graphics.getDeltaTime());

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (GameObject obj : world.getGameObjects()) {
            shapes.rect(obj.getX(), obj.getY(), obj.getWidth(), obj.getHeight());
        }
        shapes.end();
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }
}
