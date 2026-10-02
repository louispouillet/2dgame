package io.github.game2d.render;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.game2d.arena.Arena;
import io.github.game2d.arena.TileType;
import io.github.game2d.brawler.Brawler;
import io.github.game2d.collision.Circle;
import io.github.game2d.collision.Rect;
import io.github.game2d.model.GameObject;
import io.github.game2d.world.GameWorld;

public class WorldRenderer {
    private static final Color FLOOR_COLOR = new Color(0.30f, 0.30f, 0.36f, 1f);
    private static final Color WALL_COLOR = new Color(0.55f, 0.40f, 0.30f, 1f);
    private static final Color BUSH_COLOR = new Color(0.20f, 0.55f, 0.25f, 1f);
    private static final Color WATER_COLOR = new Color(0.20f, 0.45f, 0.80f, 1f);

    private static final float BAR_WIDTH = 0.8f;
    private static final float BAR_HEIGHT = 0.1f;
    private static final float BAR_GAP = 0.1f;
    private static final float AMMO_BAR_HEIGHT = 0.08f;
    private static final Color BAR_FILL = new Color(0.25f, 0.85f, 0.35f, 1f);
    private static final Color AMMO_FILL = new Color(1f, 0.55f, 0.1f, 1f);
    private static final Color AMMO_RELOAD_FILL = new Color(0.6f, 0.38f, 0.1f, 1f);
    private static final Color BAR_OUTLINE = new Color(0f, 0f, 0f, 1f);

    private static final String FONT_FILE = "fonts/LiberationSans-Bold.ttf";
    private static final int FONT_PIXEL_SIZE = 28;
    private static final float FONT_BORDER = 1.5f;
    private static final float FONT_SCALE = 0.006f;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = createFont();
    private final GlyphLayout layout = new GlyphLayout();
    private final FitViewport viewport = new FitViewport(16, 9);

    public WorldRenderer() {
        shapes.setAutoShapeType(true);
        font.getData().setScale(FONT_SCALE);
        font.setUseIntegerPositions(false);
    }

    private static BitmapFont createFont() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(FONT_FILE));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = FONT_PIXEL_SIZE;
        parameter.color = Color.WHITE;
        parameter.borderWidth = FONT_BORDER;
        parameter.borderColor = Color.BLACK;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        BitmapFont generated = generator.generateFont(parameter);
        generator.dispose();
        return generated;
    }

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
        for (GameObject obj : world.getGameObjects()) {
            if (obj instanceof Brawler brawler) {
                drawHealthBar(brawler);
                drawAmmoFill(brawler);
            }
        }
        shapes.set(ShapeRenderer.ShapeType.Line);
        for (GameObject obj : world.getGameObjects()) {
            if (obj instanceof Brawler brawler) {
                drawHealthOutline(brawler);
                drawAmmoOutline(brawler);
            }
        }
        shapes.end();
        drawHealthTexts(world);
    }

    private void drawHealthTexts(GameWorld world) {
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        for (GameObject obj : world.getGameObjects()) {
            if (obj instanceof Brawler brawler) {
                layout.setText(font, String.valueOf(brawler.getHealth()));
                float centerX = brawler.getX();
                float centerY = healthBarBottom(brawler) + BAR_HEIGHT;
                float x = centerX - layout.width / 2;
                float y = centerY + layout.height / 2;
                font.draw(batch, layout, x, y);
            }
        }
        batch.end();
    }

    private static float barLeft(Brawler brawler) {
        return brawler.getX() - BAR_WIDTH / 2;
    }

    private static float ammoBarBottom(Brawler brawler) {
        return brawler.getY() + brawler.getHitbox().radius() + BAR_GAP;
    }

    private static float healthBarBottom(Brawler brawler) {
        return ammoBarBottom(brawler) + AMMO_BAR_HEIGHT + BAR_GAP / 2;
    }

    private void drawHealthBar(Brawler brawler) {
        float left = barLeft(brawler);
        float bottom = healthBarBottom(brawler);
        float ratio = (float) brawler.getHealth() / brawler.getMaxHealth();
        shapes.setColor(BAR_OUTLINE);
        shapes.rect(left, bottom, BAR_WIDTH, BAR_HEIGHT);
        shapes.setColor(BAR_FILL);
        shapes.rect(left, bottom, BAR_WIDTH * ratio, BAR_HEIGHT);
    }

    private void drawHealthOutline(Brawler brawler) {
        shapes.setColor(BAR_OUTLINE);
        shapes.rect(barLeft(brawler), healthBarBottom(brawler), BAR_WIDTH, BAR_HEIGHT);
    }

    private void drawAmmoFill(Brawler brawler) {
        float segmentWidth = BAR_WIDTH / brawler.getMaxAmmo();
        shapes.setColor(AMMO_FILL);
        for (int i = 0; i < brawler.getAmmo(); i++) {
            shapes.rect(barLeft(brawler) + i * segmentWidth, ammoBarBottom(brawler), segmentWidth, AMMO_BAR_HEIGHT);
        }
        if (brawler.getAmmo() < brawler.getMaxAmmo()) {
            shapes.setColor(AMMO_RELOAD_FILL);
            shapes.rect(barLeft(brawler) + brawler.getAmmo() * segmentWidth, ammoBarBottom(brawler),
                segmentWidth * brawler.getReloadProgress(), AMMO_BAR_HEIGHT);
        }
    }

    private void drawAmmoOutline(Brawler brawler) {
        float segmentWidth = BAR_WIDTH / brawler.getMaxAmmo();
        shapes.setColor(BAR_OUTLINE);
        for (int i = 0; i < brawler.getMaxAmmo(); i++) {
            shapes.rect(barLeft(brawler) + i * segmentWidth, ammoBarBottom(brawler), segmentWidth, AMMO_BAR_HEIGHT);
        }
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
        batch.dispose();
        font.dispose();
    }
}
