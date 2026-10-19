package io.github.game2d.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.ArenaLoader;
import java.io.IOException;

public class Main extends Game {
    private static final String DEFAULT_HOST = "localhost";

    private final String host;
    private Skin skin;
    private GameSession session;
    // Lives as long as the session: the server keeps the highest input number of a connection
    private InputSequence inputSequence = new InputSequence();

    public Main() {
        this(null);
    }

    public Main(String host) {
        this.host = host;
    }

    @Override
    public void create() {
        skin = UiSkin.create();
        showScreen(new NameScreen(this, skin, host == null ? DEFAULT_HOST : host));
    }

    public void showScreen(Screen next) {
        Screen previous = getScreen();
        setScreen(next);
        if (previous != null) {
            previous.dispose();
        }
    }

    public GameSession connect(String address) throws IOException {
        closeSession();
        session = new RemoteSession(address);
        return session;
    }

    public GameSession startSolo(String name) {
        closeSession();
        Arena arena = ArenaLoader.loadResource(ArenaLoader.DEFAULT_MAP);
        LocalSession solo = new LocalSession(arena);
        session = solo;
        solo.join(name);
        if (solo.playerId() != -1) {
            solo.start();
            showScreen(new GameScreen(this, skin, solo, arena, inputSequence));
        }
        return solo;
    }

    public void play(String map) {
        showScreen(new GameScreen(this, skin, session, ArenaLoader.loadResource(map), inputSequence));
    }

    private void closeSession() {
        if (session != null) {
            session.close();
            session = null;
        }
        inputSequence = new InputSequence();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (getScreen() != null) {
            getScreen().dispose();
        }
        closeSession();
        skin.dispose();
    }
}
