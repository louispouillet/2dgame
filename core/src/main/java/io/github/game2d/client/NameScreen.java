package io.github.game2d.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.game2d.server.Lobby;
import java.io.IOException;

public class NameScreen extends ScreenAdapter {
    private static final float FIELD_WIDTH = 360f;
    private static final float SPACING = 16f;
    private static final Color ERROR_COLOR = new Color(1f, 0.45f, 0.45f, 1f);

    private final Main main;
    private final Skin skin;
    private final Stage stage = new Stage(new ScreenViewport());
    private final TextField nameField;
    private final TextField addressField;
    private final Label message;
    private GameSession session;
    private String connectedAddress;
    private boolean waitingForAnswer;

    public NameScreen(Main main, Skin skin, String address) {
        this.main = main;
        this.skin = skin;
        nameField = new TextField("", skin);
        nameField.setMessageText("Nickname");
        nameField.setMaxLength(Lobby.MAX_NAME_LENGTH);
        nameField.setTextFieldFilter((field, c) -> Character.isLetterOrDigit(c) || c == '_' || c == '-');
        nameField.setTextFieldListener((field, c) -> {
            if (c == '\r' || c == '\n') {
                join();
            }
        });
        addressField = new TextField(address, skin);
        message = new Label("", skin);
        message.setColor(ERROR_COLOR);
        TextButton joinButton = new TextButton("Join", skin, UiSkin.BLUE);
        TextButton soloButton = new TextButton("Solo", skin);
        joinButton.addListener(onClick(this::join));
        soloButton.addListener(onClick(this::playSolo));

        Table root = new Table();
        root.setFillParent(true);
        root.defaults().pad(SPACING / 2);
        root.add(new Label("Brawl POC", skin, UiSkin.TITLE)).colspan(2).padBottom(SPACING * 2).row();
        root.add(new Label("Nickname", skin)).right();
        root.add(nameField).width(FIELD_WIDTH).row();
        root.add(new Label("Server", skin)).right();
        root.add(addressField).width(FIELD_WIDTH).row();
        Table buttons = new Table();
        buttons.defaults().pad(SPACING / 2).minWidth(FIELD_WIDTH / 2);
        buttons.add(joinButton);
        buttons.add(soloButton);
        root.add(buttons).colspan(2).padTop(SPACING).row();
        root.add(message).colspan(2);
        stage.addActor(root);
    }

    private static ChangeListener onClick(Runnable action) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        };
    }

    private void join() {
        String address = addressField.getText().strip();
        if (session == null || !address.equals(connectedAddress)) {
            try {
                session = main.connect(address);
                connectedAddress = address;
            } catch (IOException e) {
                session = null;
                message.setText("Cannot reach the server " + address);
                return;
            }
        }
        message.setText("");
        session.join(nameField.getText());
        waitingForAnswer = true;
    }

    private void playSolo() {
        GameSession solo = main.startSolo(nameField.getText());
        if (solo.rejection() != null) {
            message.setText(RefusalMessages.describe(solo.rejection()));
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        stage.setKeyboardFocus(nameField);
    }

    @Override
    public void render(float delta) {
        if (waitingForAnswer && session.playerId() != -1) {
            main.showScreen(new LobbyScreen(main, skin, session));
            return;
        }
        if (waitingForAnswer && session.rejection() != null) {
            message.setText(RefusalMessages.describe(session.rejection()));
            waitingForAnswer = false;
        }
        ScreenUtils.clear(UiSkin.BACKGROUND);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
