package io.github.game2d.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public final class UiSkin {
    public static final String TITLE = "title";
    public static final String BLUE = "blue";
    public static final String RED = "red";
    public static final String PANEL = "panel";
    public static final Color BACKGROUND = new Color(0.10f, 0.11f, 0.15f, 1f);

    private static final String FONT_FILE = "fonts/LiberationSans-Bold.ttf";
    private static final int TEXT_SIZE = 24;
    private static final int TITLE_SIZE = 56;
    private static final String WHITE = "white";
    private static final float PADDING = 12f;

    private static final Color PANEL_COLOR = new Color(0.17f, 0.18f, 0.24f, 1f);
    private static final Color FIELD_COLOR = new Color(0.24f, 0.26f, 0.33f, 1f);
    private static final Color SELECTION_COLOR = new Color(0.35f, 0.45f, 0.75f, 1f);
    private static final Color DISABLED_COLOR = new Color(0.22f, 0.22f, 0.26f, 1f);
    private static final Color DISABLED_TEXT = new Color(0.5f, 0.5f, 0.55f, 1f);
    private static final Color GRAY = new Color(0.35f, 0.37f, 0.45f, 1f);
    private static final Color BLUE_COLOR = new Color(0.20f, 0.45f, 0.90f, 1f);
    private static final Color RED_COLOR = new Color(0.85f, 0.25f, 0.25f, 1f);

    private UiSkin() {
    }

    public static Skin create() {
        Skin skin = new Skin();
        skin.add(WHITE, whiteTexture());
        BitmapFont text = font(TEXT_SIZE);
        BitmapFont title = font(TITLE_SIZE);
        skin.add("default-font", text);
        skin.add(TITLE + "-font", title);

        skin.add("default", new Label.LabelStyle(text, Color.WHITE));
        skin.add(TITLE, new Label.LabelStyle(title, Color.WHITE));
        skin.add(PANEL, padded(skin, PANEL_COLOR), Drawable.class);

        skin.add("default", button(skin, text, GRAY));
        skin.add(BLUE, button(skin, text, BLUE_COLOR));
        skin.add(RED, button(skin, text, RED_COLOR));
        skin.add("default", textField(skin, text));
        skin.add("default", selectBox(skin, text));
        return skin;
    }

    private static Texture whiteTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private static BitmapFont font(int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(FONT_FILE));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "éèêëàâäîïôöùûüçÉÈÊÀÂÎÔÙÛÇ…";
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        return font;
    }

    private static Drawable padded(Skin skin, Color color) {
        Drawable drawable = skin.newDrawable(WHITE, color);
        if (drawable instanceof BaseDrawable base) {
            base.setLeftWidth(PADDING);
            base.setRightWidth(PADDING);
            base.setTopHeight(PADDING / 2);
            base.setBottomHeight(PADDING / 2);
        }
        return drawable;
    }

    private static TextButton.TextButtonStyle button(Skin skin, BitmapFont font, Color color) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.disabledFontColor = DISABLED_TEXT;
        style.up = padded(skin, color);
        style.over = padded(skin, color.cpy().lerp(Color.WHITE, 0.15f));
        style.down = padded(skin, color.cpy().lerp(Color.BLACK, 0.25f));
        style.disabled = padded(skin, DISABLED_COLOR);
        return style;
    }

    private static TextField.TextFieldStyle textField(Skin skin, BitmapFont font) {
        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.messageFont = font;
        style.messageFontColor = DISABLED_TEXT;
        style.background = padded(skin, FIELD_COLOR);
        style.focusedBackground = padded(skin, FIELD_COLOR.cpy().lerp(Color.WHITE, 0.08f));
        style.selection = skin.newDrawable(WHITE, SELECTION_COLOR);
        Drawable cursor = skin.newDrawable(WHITE, Color.WHITE);
        cursor.setMinWidth(2f);
        style.cursor = cursor;
        return style;
    }

    private static SelectBox.SelectBoxStyle selectBox(Skin skin, BitmapFont font) {
        List.ListStyle list = new List.ListStyle(font, Color.WHITE, Color.LIGHT_GRAY, padded(skin, SELECTION_COLOR));
        list.background = padded(skin, FIELD_COLOR);
        ScrollPane.ScrollPaneStyle scroll = new ScrollPane.ScrollPaneStyle();
        scroll.background = skin.newDrawable(WHITE, FIELD_COLOR);
        SelectBox.SelectBoxStyle style = new SelectBox.SelectBoxStyle(font, Color.WHITE, padded(skin, FIELD_COLOR), scroll, list);
        style.disabledFontColor = DISABLED_TEXT;
        style.backgroundDisabled = padded(skin, DISABLED_COLOR);
        return style;
    }
}
