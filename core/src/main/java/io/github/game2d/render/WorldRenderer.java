package io.github.game2d.render;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.game2d.arena.Arena;
import io.github.game2d.arena.TileType;
import io.github.game2d.collision.Circle;
import io.github.game2d.collision.Rect;
import io.github.game2d.model.GameObject;
import io.github.game2d.world.GameWorld;

public class WorldRenderer {
    private static final Color FLOOR_COLOR = new Color(0.30f, 0.30f, 0.36f, 1f);
    private static final Color WALL_COLOR = new Color(0.55f, 0.40f, 0.30f, 1f);
    private static final Color BUSH_COLOR = new Color(0.20f, 0.55f, 0.25f, 1f);
    private static final Color WATER_COLOR = new Color(0.20f, 0.45f, 0.80f, 1f);

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final FitViewport viewport = new FitViewport(16, 9);

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void render(GameWorld world, Arena arena) {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawArena(arena);
        shapes.setColor(Color.WHITE);
        for (GameObject obj : world.getGameObjects()) {
            switch (obj.getHitbox()) {
                case Circle c -> shapes.circle(obj.getX(), obj.getY(), c.radius(), 24);
                case Rect r -> shapes.rect(obj.getX() - r.width() / 2, obj.getY() - r.height() / 2, r.width(), r.height());
            }
        }
        shapes.end();
    }

    private void drawArena(Arena arena) {
        for (int row = 0; row < arena.getHeight(); row++) {
            for (int col = 0; col < arena.getWidth(); col++) {
                shapes.setColor(colorOf(arena.getTile(col, row)));
                shapes.rect(col, row, 1, 1);
            }
        }
    }

    private static Color colorOf(TileType tile) {
        return switch (tile) {
            case FLOOR -> FLOOR_COLOR;
            case WALL -> WALL_COLOR;
            case BUSH -> BUSH_COLOR;
            case WATER -> WATER_COLOR;
        };
    }

    public FitViewport getViewport() {
        return viewport;
    }

    public void dispose() {
        shapes.dispose();
    }
}
