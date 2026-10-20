package io.github.game2d.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * Draws the match information over the 3D view: scores and round at the top, the countdown or the result
 * at the center, and the name of the ally I watch at the bottom.
 */
public class MatchHud {
    private static final float TOP_MARGIN = 20f;
    private static final float BOTTOM_MARGIN = 40f;
    private static final float SCORE_GAP = 40f;
    private static final float PADDING = 14f;
    private static final Color ALLY_COLOR = new Color(0.45f, 0.70f, 1f, 1f);
    private static final Color ENEMY_COLOR = new Color(1f, 0.45f, 0.45f, 1f);

    private final SpriteBatch batch = new SpriteBatch();
    private final GlyphLayout layout = new GlyphLayout();
    private final Matrix4 projection = new Matrix4();
    private final BitmapFont textFont;
    private final BitmapFont titleFont;
    private final Drawable shade;

    /** Creates the HUD with the fonts and the shade of the skin. The skin keeps ownership of them. */
    public MatchHud(Skin skin) {
        textFont = skin.getFont("default-font");
        titleFont = skin.getFont(UiSkin.TITLE + "-font");
        shade = skin.getDrawable(UiSkin.SHADE);
    }

    /**
     * Draws the HUD. Does nothing before the match state is known.
     *
     * @param view         what to show
     * @param watchingText text for the bottom of the screen, may be empty
     */
    public void draw(MatchView view, String watchingText) {
        if (!view.hasMatch()) {
            return;
        }
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        projection.setToOrtho2D(0f, 0f, width, height);
        batch.setProjectionMatrix(projection);
        batch.begin();
        drawTopBar(view, width / 2f, height - TOP_MARGIN);
        drawCentered(titleFont, view.centerText(), toneColor(view.centerTone()), width / 2f, height / 2f);
        drawCentered(textFont, watchingText, Color.WHITE, width / 2f, BOTTOM_MARGIN);
        batch.end();
    }

    /** Draws the shaded bar with my score, the round and the enemy score. */
    private void drawTopBar(MatchView view, float centerX, float top) {
        layout.setText(textFont, view.roundLabel());
        float halfRound = layout.width / 2f;
        layout.setText(titleFont, "0");
        float barWidth = 2f * (halfRound + SCORE_GAP + layout.width) + 2f * PADDING;
        float barHeight = layout.height + 2f * PADDING;
        shade.draw(batch, centerX - barWidth / 2f, top - layout.height - PADDING, barWidth, barHeight);
        drawAt(titleFont, String.valueOf(view.myScore()), ALLY_COLOR, centerX - halfRound - SCORE_GAP, top, true);
        drawAt(textFont, view.roundLabel(), Color.WHITE, centerX - halfRound, top, false);
        drawAt(titleFont, String.valueOf(view.enemyScore()), ENEMY_COLOR, centerX + halfRound + SCORE_GAP, top, false);
    }

    /** Draws a text starting (or ending, if right aligned) at a point. */
    private void drawAt(BitmapFont font, String text, Color color, float x, float top, boolean alignRight) {
        font.setColor(color);
        layout.setText(font, text);
        font.draw(batch, layout, alignRight ? x - layout.width : x, top);
        font.setColor(Color.WHITE);
    }

    /** Draws a text centered on a point, on a shade. Does nothing for an empty text. */
    private void drawCentered(BitmapFont font, String text, Color color, float centerX, float centerY) {
        if (text.isEmpty()) {
            return;
        }
        layout.setText(font, text);
        shade.draw(batch, centerX - layout.width / 2f - PADDING, centerY - layout.height / 2f - PADDING,
            layout.width + 2f * PADDING, layout.height + 2f * PADDING);
        font.setColor(color);
        font.draw(batch, layout, centerX - layout.width / 2f, centerY + layout.height / 2f);
        font.setColor(Color.WHITE);
    }

    /** Returns the color of a tone. */
    private static Color toneColor(MatchView.Tone tone) {
        return switch (tone) {
            case WIN -> ALLY_COLOR;
            case LOSS -> ENEMY_COLOR;
            case NEUTRAL -> Color.WHITE;
        };
    }

    /** Frees the batch. */
    public void dispose() {
        batch.dispose();
    }
}
