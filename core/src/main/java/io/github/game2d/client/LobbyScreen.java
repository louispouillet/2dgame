package io.github.game2d.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.game2d.model.Team;
import io.github.game2d.protocol.LobbyState;
import io.github.game2d.server.Lobby;
import java.util.List;

public class LobbyScreen extends ScreenAdapter {
    private static final float PANEL_WIDTH = 320f;
    private static final float PANEL_HEIGHT = 200f;
    private static final float SPACING = 16f;

    private final Main main;
    private final Skin skin;
    private final GameSession session;
    private final Stage stage = new Stage(new ScreenViewport());
    private final Label blueTitle;
    private final Label redTitle;
    private final Table bluePlayers = new Table();
    private final Table redPlayers = new Table();
    private final TextButton joinBlue;
    private final TextButton joinRed;
    private final SelectBox<String> mapBox;
    private final TextButton startButton;
    private final Label status;
    private LobbyState shown;
    private List<String> maps = List.of();
    private boolean updatingMapBox;

    public LobbyScreen(Main main, Skin skin, GameSession session) {
        this.main = main;
        this.skin = skin;
        this.session = session;
        blueTitle = new Label("", skin);
        redTitle = new Label("", skin);
        joinBlue = new TextButton("Join Blue", skin, UiSkin.BLUE);
        joinRed = new TextButton("Join Red", skin, UiSkin.RED);
        mapBox = new SelectBox<>(skin);
        startButton = new TextButton("Start", skin);
        status = new Label("", skin);

        joinBlue.addListener(onClick(() -> session.chooseTeam(Team.BLUE)));
        joinRed.addListener(onClick(() -> session.chooseTeam(Team.RED)));
        startButton.addListener(onClick(session::start));
        mapBox.addListener(onClick(this::chooseMap));

        Table root = new Table();
        root.setFillParent(true);
        root.defaults().pad(SPACING / 2);
        root.add(new Label("Lobby", skin, UiSkin.TITLE)).colspan(2).padBottom(SPACING).row();
        root.add(panel(blueTitle, bluePlayers)).size(PANEL_WIDTH, PANEL_HEIGHT);
        root.add(panel(redTitle, redPlayers)).size(PANEL_WIDTH, PANEL_HEIGHT).row();
        root.add(joinBlue).fillX();
        root.add(joinRed).fillX().row();
        Table map = new Table();
        map.add(new Label("Map", skin)).padRight(SPACING);
        map.add(mapBox).width(PANEL_WIDTH / 2);
        root.add(map).left();
        root.add(startButton).right().minWidth(PANEL_WIDTH / 2).row();
        root.add(status).colspan(2).padTop(SPACING);
        stage.addActor(root);
    }

    private Table panel(Label title, Table players) {
        Table panel = new Table();
        panel.setBackground(skin.getDrawable(UiSkin.PANEL));
        panel.top();
        panel.add(title).padBottom(SPACING).row();
        panel.add(players).expandX().fillX();
        return panel;
    }

    private static ChangeListener onClick(Runnable action) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        };
    }

    private void chooseMap() {
        int index = mapBox.getSelectedIndex();
        if (!updatingMapBox && index >= 0 && index < maps.size()) {
            session.chooseMap(maps.get(index));
        }
    }

    private void refresh(LobbyState state) {
        LobbyView view = new LobbyView(state, session.playerId());
        blueTitle.setText("BLUE " + view.count(Team.BLUE) + "/" + Lobby.MAX_TEAM_SIZE);
        redTitle.setText("RED " + view.count(Team.RED) + "/" + Lobby.MAX_TEAM_SIZE);
        fill(bluePlayers, view.playerLabels(Team.BLUE));
        fill(redPlayers, view.playerLabels(Team.RED));
        joinBlue.setDisabled(!view.canJoin(Team.BLUE));
        joinRed.setDisabled(!view.canJoin(Team.RED));
        startButton.setDisabled(!view.canStart());
        status.setText(view.status());
        updateMapBox(state, view.canChooseMap());
    }

    private void fill(Table players, List<String> labels) {
        players.clear();
        for (String label : labels) {
            players.add(new Label(label, skin)).left().row();
        }
    }

    private void updateMapBox(LobbyState state, boolean enabled) {
        updatingMapBox = true;
        if (!state.maps().equals(maps)) {
            maps = List.copyOf(state.maps());
            Array<String> labels = new Array<>();
            maps.forEach(map -> labels.add(LobbyView.mapLabel(map)));
            mapBox.setItems(labels);
        }
        mapBox.setSelectedIndex(maps.indexOf(state.map()));
        mapBox.setDisabled(!enabled);
        updatingMapBox = false;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        LobbyState state = session.channel().latestLobby();
        if (state != null && state.started()) {
            main.play(state.map());
            return;
        }
        if (state != null && state != shown) {
            refresh(state);
            shown = state;
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
