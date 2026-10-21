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

/**
 * Draws, under each brawler, its ammo segments, its health bar with the value and its nickname.
 * Green is me, blue my allies, red my enemies.
 */
public class BrawlerHud {
    private static final float BAR_WIDTH = 0.8f;
    private static final float BAR_HEIGHT = 0.1f;
    private static final float BAR_GAP = 0.1f;
    private static final float AMMO_BAR_HEIGHT = 0.08f;
    private static final Color MY_BAR_FILL = new Color(0.25f, 0.85f, 0.35f, 1f);
    private static final Color ALLY_BAR_FILL = new Color(0.20f, 0.50f, 1f, 1f);
    private static final Color ENEMY_BAR_FILL = new Color(0.90f, 0.25f, 0.25f, 1f);
    private static final Color AMMO_FILL = new Color(1f, 0.55f, 0.1f, 1f);
    private static final Color AMMO_RELOAD_FILL = new Color(0.6f, 0.38f, 0.1f, 1f);
    private static final Color BAR_OUTLINE = new Color(0f, 0f, 0f, 1f);
    private static final Color MY_NAME = new Color(0.55f, 1f, 0.6f, 1f);
    private static final Color ALLY_NAME = new Color(0.55f, 0.75f, 1f, 1f);
    private static final Color ENEMY_NAME = new Color(1f, 0.55f, 0.55f, 1f);

    private static final String FONT_FILE = "fonts/LiberationSans-Bold.ttf";
    private static final int FONT_PIXEL_SIZE = 28;
    private static final float FONT_BORDER = 1.5f;
    private static final float FONT_SCALE = 0.006f;
    private static final float NAME_GAP = 0.06f;

    private final BitmapFont font = createFont();
    private final GlyphLayout layout = new GlyphLayout();
    private final PlayerRoster roster;

    /**
     * Creates a HUD; it loads its own font.
     *
     * @param roster gives the nicknames and the teams
     */
    public BrawlerHud(PlayerRoster roster) {
        this.roster = roster;
        font.setUseIntegerPositions(false);
    }

    /** Generates the font with a dark outline. */
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

    /**
     * Draws the HUD of every brawler.
     *
     * @param shapes  renderer for the bars, with its projection already set to the screen
     * @param batch   renderer for the texts, with its projection already set to the screen
     * @param anchors where to draw each brawler
     */
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
            drawName(batch, anchor);
        }
        batch.end();
    }

    /** Draws the health value on the bar. */
    private void drawHealthText(SpriteBatch batch, HudAnchor anchor) {
        font.getData().setScale(FONT_SCALE * anchor.unit());
        font.setColor(Color.WHITE);
        layout.setText(font, String.valueOf(anchor.brawler().health()));
        float centerY = healthBarBottom(anchor) + BAR_HEIGHT * anchor.unit();
        float x = anchor.centerX() - layout.width / 2;
        float y = centerY + layout.height / 2;
        font.draw(batch, layout, x, y);
    }

    /** Draws the nickname above the health bar, if the roster knows it. */
    private void drawName(SpriteBatch batch, HudAnchor anchor) {
        int id = anchor.brawler().id();
        String name = roster.nameOf(id);
        if (name.isEmpty()) {
            return;
        }
        font.getData().setScale(FONT_SCALE * anchor.unit());
        font.setColor(colorFor(id, MY_NAME, ALLY_NAME, ENEMY_NAME));
        layout.setText(font, name);
        float barTop = healthBarBottom(anchor) + BAR_HEIGHT * anchor.unit();
        float healthTextTop = barTop + layout.height / 2;
        float x = anchor.centerX() - layout.width / 2;
        float y = healthTextTop + NAME_GAP * anchor.unit() + layout.height;
        font.draw(batch, layout, x, y);
    }

    /** Green for the player's brawler, blue for allies, red for enemies. */
    private Color colorFor(int brawlerId, Color me, Color ally, Color enemy) {
        if (roster.isMe(brawlerId)) {
            return me;
        }
        return roster.isAlly(brawlerId) ? ally : enemy;
    }

    /** Returns the width of the bars, in pixels. */
    private static float barWidth(HudAnchor anchor) {
        return BAR_WIDTH * anchor.unit();
    }

    /** Returns the left edge of the bars, in pixels. */
    private static float barLeft(HudAnchor anchor) {
        return anchor.centerX() - barWidth(anchor) / 2;
    }

    /** Returns the bottom of the ammo bar, in pixels. */
    private static float ammoBarBottom(HudAnchor anchor) {
        return anchor.bottomY() + BAR_GAP * anchor.unit();
    }

    /** Returns the bottom of the health bar, in pixels. */
    private static float healthBarBottom(HudAnchor anchor) {
        return ammoBarBottom(anchor) + (AMMO_BAR_HEIGHT + BAR_GAP / 2) * anchor.unit();
    }

    /** Draws the background and the filled part of the health bar. */
    private void drawHealthBar(ShapeRenderer shapes, HudAnchor anchor) {
        BrawlerState brawler = anchor.brawler();
        float ratio = (float) brawler.health() / brawler.maxHealth();
        float height = BAR_HEIGHT * anchor.unit();
        shapes.setColor(BAR_OUTLINE);
        shapes.rect(barLeft(anchor), healthBarBottom(anchor), barWidth(anchor), height);
        shapes.setColor(colorFor(brawler.id(), MY_BAR_FILL, ALLY_BAR_FILL, ENEMY_BAR_FILL));
        shapes.rect(barLeft(anchor), healthBarBottom(anchor), barWidth(anchor) * ratio, height);
    }

    /** Draws the outline of the health bar. */
    private static void drawHealthOutline(ShapeRenderer shapes, HudAnchor anchor) {
        shapes.setColor(BAR_OUTLINE);
        shapes.rect(barLeft(anchor), healthBarBottom(anchor), barWidth(anchor), BAR_HEIGHT * anchor.unit());
    }

    /** Draws the full ammo segments, and the progress of the reload in the next one. */
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

    /** Draws the outline of each ammo segment. */
    private static void drawAmmoOutline(ShapeRenderer shapes, HudAnchor anchor) {
        float segmentWidth = barWidth(anchor) / anchor.brawler().maxAmmo();
        shapes.setColor(BAR_OUTLINE);
        for (int i = 0; i < anchor.brawler().maxAmmo(); i++) {
            shapes.rect(barLeft(anchor) + i * segmentWidth, ammoBarBottom(anchor), segmentWidth,
                AMMO_BAR_HEIGHT * anchor.unit());
        }
    }

    /** Frees the font. */
    public void dispose() {
        font.dispose();
    }
}
