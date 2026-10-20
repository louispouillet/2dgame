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
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.protocol.LobbyState;
import io.github.game2d.server.Lobby;
import java.util.List;

/**
 * The lobby: the two teams, the choice of brawler and map, and the start button.
 * It sends the commands of the player to the session and shows the lobby state it receives.
 * The match screen replaces it as soon as the lobby says the match started.
 */
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
    private final SelectBox<String> brawlerBox;
    private final SelectBox<String> mapBox;
    private final TextButton startButton;
    private final Label status;
    private LobbyState shown;
    private List<String> maps = List.of();
    // Set while the screen itself changes a selection, so that this change is not sent back to the server
    private boolean updatingBoxes;

    /**
     * Creates the screen.
     *
     * @param main    the application, to change screen
     * @param skin    the UI skin
     * @param session the session, already joined
     */
    public LobbyScreen(Main main, Skin skin, GameSession session) {
        this.main = main;
        this.skin = skin;
        this.session = session;
        blueTitle = new Label("", skin);
        redTitle = new Label("", skin);
        joinBlue = new TextButton("Join Blue", skin, UiSkin.BLUE);
        joinRed = new TextButton("Join Red", skin, UiSkin.RED);
        brawlerBox = new SelectBox<>(skin);
        // Filled before its listener is added: setItems selects the first item, which fires a change event
        brawlerBox.setItems(brawlerLabels());
        mapBox = new SelectBox<>(skin);
        startButton = new TextButton("Start", skin);
        status = new Label("", skin);

        joinBlue.addListener(onClick(() -> session.chooseTeam(Team.BLUE)));
        joinRed.addListener(onClick(() -> session.chooseTeam(Team.RED)));
        startButton.addListener(onClick(session::start));
        brawlerBox.addListener(onClick(this::chooseBrawler));
        mapBox.addListener(onClick(this::chooseMap));

        Table root = new Table();
        root.setFillParent(true);
        root.defaults().pad(SPACING / 2);
        root.add(new Label("Lobby", skin, UiSkin.TITLE)).colspan(2).padBottom(SPACING).row();
        root.add(panel(blueTitle, bluePlayers)).size(PANEL_WIDTH, PANEL_HEIGHT);
        root.add(panel(redTitle, redPlayers)).size(PANEL_WIDTH, PANEL_HEIGHT).row();
        root.add(joinBlue).fillX();
        root.add(joinRed).fillX().row();
        root.add(choice("Brawler", brawlerBox)).left();
        root.add(choice("Map", mapBox)).right().row();
        root.add(startButton).colspan(2).right().minWidth(PANEL_WIDTH / 2).row();
        root.add(status).colspan(2).padTop(SPACING);
        stage.addActor(root);
    }

    /** Builds the box of a team: a title and the list of its players. */
    private Table panel(Label title, Table players) {
        Table panel = new Table();
        panel.setBackground(skin.getDrawable(UiSkin.PANEL));
        panel.top();
        panel.add(title).padBottom(SPACING).row();
        panel.add(players).expandX().fillX();
        return panel;
    }

    /** Returns the names of all the brawler types, in the order of {@link BrawlerType#values()}. */
    private static Array<String> brawlerLabels() {
        Array<String> labels = new Array<>();
        for (BrawlerType type : BrawlerType.values()) {
            labels.add(LobbyView.brawlerLabel(type));
        }
        return labels;
    }

    /** Builds a label followed by a list box. */
    private Table choice(String title, SelectBox<String> box) {
        Table choice = new Table();
        choice.add(new Label(title, skin)).padRight(SPACING);
        choice.add(box).width(PANEL_WIDTH / 2);
        return choice;
    }

    /** Returns a listener that runs an action when the widget changes. */
    private static ChangeListener onClick(Runnable action) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        };
    }

    /** Sends the brawler selected by the player (not the changes made by {@link #refresh}). */
    private void chooseBrawler() {
        int index = brawlerBox.getSelectedIndex();
        if (!updatingBoxes && index >= 0) {
            session.chooseBrawler(BrawlerType.values()[index]);
        }
    }

    /** Sends the map selected by the player (not the changes made by {@link #refresh}). */
    private void chooseMap() {
        int index = mapBox.getSelectedIndex();
        if (!updatingBoxes && index >= 0 && index < maps.size()) {
            session.chooseMap(maps.get(index));
        }
    }

    /** Updates every widget from a new lobby state. */
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
        updatingBoxes = true;
        updateBrawlerBox(view.myBrawler());
        updateMapBox(state, view.canChooseMap());
        updatingBoxes = false;
    }

    /** Replaces the content of a team list. */
    private void fill(Table players, List<String> labels) {
        players.clear();
        for (String label : labels) {
            players.add(new Label(label, skin)).left().row();
        }
    }

    /** Selects my brawler in the list. */
    private void updateBrawlerBox(BrawlerType myBrawler) {
        if (myBrawler != null) {
            brawlerBox.setSelectedIndex(myBrawler.ordinal());
        }
    }

    /** Updates the map list and its selection; only the leader can use it. */
    private void updateMapBox(LobbyState state, boolean enabled) {
        if (!state.maps().equals(maps)) {
            maps = List.copyOf(state.maps());
            Array<String> labels = new Array<>();
            maps.forEach(map -> labels.add(LobbyView.mapLabel(map)));
            mapBox.setItems(labels);
        }
        mapBox.setSelectedIndex(maps.indexOf(state.map()));
        mapBox.setDisabled(!enabled);
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
