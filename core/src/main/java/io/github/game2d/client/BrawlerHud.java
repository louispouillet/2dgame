package io.github.game2d.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.game2d.protocol.BrawlerState;
import java.util.List;

public class BrawlerHud {
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

    private final BitmapFont font = createFont();
    private final GlyphLayout layout = new GlyphLayout();

    public BrawlerHud() {
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

    public void draw(ShapeRenderer shapes, SpriteBatch batch, List<HudAnchor> anchors) {
        shapes.setAutoShapeType(true);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (HudAnchor anchor : anchors) {
            drawHealthBar(shapes, anchor);
            drawAmmoFill(shapes, anchor);
        }
        shapes.set(ShapeRenderer.ShapeType.Line);
        for (HudAnchor anchor : anchors) {
            drawHealthOutline(shapes, anchor);
            drawAmmoOutline(shapes, anchor);
        }
        shapes.end();

        batch.begin();
        for (HudAnchor anchor : anchors) {
            drawHealthText(batch, anchor);
        }
        batch.end();
    }

    private void drawHealthText(SpriteBatch batch, HudAnchor anchor) {
        font.getData().setScale(FONT_SCALE * anchor.unit());
        layout.setText(font, String.valueOf(anchor.brawler().health()));
        float centerY = healthBarBottom(anchor) + BAR_HEIGHT * anchor.unit();
        float x = anchor.centerX() - layout.width / 2;
        float y = centerY + layout.height / 2;
        font.draw(batch, layout, x, y);
    }

    private static float barWidth(HudAnchor anchor) {
        return BAR_WIDTH * anchor.unit();
    }

    private static float barLeft(HudAnchor anchor) {
        return anchor.centerX() - barWidth(anchor) / 2;
    }

    private static float ammoBarBottom(HudAnchor anchor) {
        return anchor.bottomY() + BAR_GAP * anchor.unit();
    }

    private static float healthBarBottom(HudAnchor anchor) {
        return ammoBarBottom(anchor) + (AMMO_BAR_HEIGHT + BAR_GAP / 2) * anchor.unit();
    }

    private static void drawHealthBar(ShapeRenderer shapes, HudAnchor anchor) {
        BrawlerState brawler = anchor.brawler();
        float ratio = (float) brawler.health() / brawler.maxHealth();
        float height = BAR_HEIGHT * anchor.unit();
        shapes.setColor(BAR_OUTLINE);
        shapes.rect(barLeft(anchor), healthBarBottom(anchor), barWidth(anchor), height);
        shapes.setColor(BAR_FILL);
        shapes.rect(barLeft(anchor), healthBarBottom(anchor), barWidth(anchor) * ratio, height);
    }

    private static void drawHealthOutline(ShapeRenderer shapes, HudAnchor anchor) {
        shapes.setColor(BAR_OUTLINE);
        shapes.rect(barLeft(anchor), healthBarBottom(anchor), barWidth(anchor), BAR_HEIGHT * anchor.unit());
    }

    private static void drawAmmoFill(ShapeRenderer shapes, HudAnchor anchor) {
        BrawlerState brawler = anchor.brawler();
        float segmentWidth = barWidth(anchor) / brawler.maxAmmo();
        float height = AMMO_BAR_HEIGHT * anchor.unit();
        shapes.setColor(AMMO_FILL);
        for (int i = 0; i < brawler.ammo(); i++) {
            shapes.rect(barLeft(anchor) + i * segmentWidth, ammoBarBottom(anchor), segmentWidth, height);
        }
        if (brawler.ammo() < brawler.maxAmmo()) {
            shapes.setColor(AMMO_RELOAD_FILL);
            shapes.rect(barLeft(anchor) + brawler.ammo() * segmentWidth, ammoBarBottom(anchor),
                segmentWidth * brawler.reloadProgress(), height);
        }
    }

    private static void drawAmmoOutline(ShapeRenderer shapes, HudAnchor anchor) {
        float segmentWidth = barWidth(anchor) / anchor.brawler().maxAmmo();
        shapes.setColor(BAR_OUTLINE);
        for (int i = 0; i < anchor.brawler().maxAmmo(); i++) {
            shapes.rect(barLeft(anchor) + i * segmentWidth, ammoBarBottom(anchor), segmentWidth,
                AMMO_BAR_HEIGHT * anchor.unit());
        }
    }

    public void dispose() {
        font.dispose();
    }
}
