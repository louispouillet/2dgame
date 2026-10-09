package io.github.game2d.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.TileType;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.ProjectileState;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.ArrayList;
import java.util.List;

public class WorldRenderer implements GameView {
    private static final Color FLOOR_COLOR = new Color(0.30f, 0.30f, 0.36f, 1f);
    private static final Color WALL_COLOR = new Color(0.55f, 0.40f, 0.30f, 1f);
    private static final Color BUSH_COLOR = new Color(0.20f, 0.55f, 0.25f, 1f);
    private static final Color SEE_THROUGH_BUSH_COLOR = new Color(0.20f, 0.55f, 0.25f, 0.4f);
    private static final Color WATER_COLOR = new Color(0.20f, 0.45f, 0.80f, 1f);

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BrawlerHud hud;
    private final FitViewport viewport;

    public WorldRenderer(float worldWidth, float worldHeight, PlayerNames names) {
        viewport = new FitViewport(worldWidth, worldHeight);
        hud = new BrawlerHud(names);
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void render(WorldSnapshot snapshot, Arena arena, int playerId) {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawArena(arena, snapshot.findBrawler(playerId));
        shapes.setColor(Color.WHITE);
        for (ProjectileState projectile : snapshot.projectiles()) {
            shapes.circle(projectile.x(), projectile.y(), projectile.radius(), 24);
        }
        for (BrawlerState brawler : snapshot.brawlers()) {
            shapes.circle(brawler.x(), brawler.y(), brawler.radius(), 24);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        List<HudAnchor> anchors = new ArrayList<>();
        for (BrawlerState brawler : snapshot.brawlers()) {
            anchors.add(new HudAnchor(brawler, brawler.x(), brawler.y() + brawler.radius(), 1f));
        }
        batch.setProjectionMatrix(viewport.getCamera().combined);
        hud.draw(shapes, batch, anchors);
    }

    private void drawArena(Arena arena, BrawlerState player) {
        for (int row = 0; row < arena.getHeight(); row++) {
            for (int col = 0; col < arena.getWidth(); col++) {
                if (player != null && arena.canSeeIntoBush(col, row, player.x(), player.y())) {
                    shapes.setColor(FLOOR_COLOR);
                    shapes.rect(col, row, 1, 1);
                    shapes.setColor(SEE_THROUGH_BUSH_COLOR);
                } else {
                    shapes.setColor(colorOf(arena.getTile(col, row)));
                }
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

    @Override
    public Vector2 screenToWorld(int screenX, int screenY) {
        return viewport.unproject(new Vector2(screenX, screenY));
    }

    public FitViewport getViewport() {
        return viewport;
    }

    public void dispose() {
        shapes.dispose();
        batch.dispose();
        hud.dispose();
    }
}
