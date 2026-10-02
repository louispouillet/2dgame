package io.github.game2d.render;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.game2d.model.GameObject;
import io.github.game2d.world.GameWorld;

public class WorldRenderer {
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final FitViewport viewport = new FitViewport(16, 9);

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void render(GameWorld world) {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (GameObject obj : world.getGameObjects()) {
            shapes.rect(obj.getX(), obj.getY(), obj.getWidth(), obj.getHeight());
        }
        shapes.end();
    }

    public FitViewport getViewport() {
        return viewport;
    }

    public void dispose() {
        shapes.dispose();
    }
}
