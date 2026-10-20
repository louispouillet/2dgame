package io.github.game2d.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.ArenaLoader;
import java.io.IOException;

/**
 * Entry point of the libGDX client. Shows one screen at a time; the session and the skin belong to it
 * and survive the changes of screen.
 */
public class Main extends Game {
    private static final String DEFAULT_HOST = "localhost";

    private final String host;
    private Skin skin;
    private GameSession session;
    // Lives as long as the session: the server keeps the highest input number of a connection
    private InputSequence inputSequence = new InputSequence();

    /** Creates a client that proposes {@code localhost} as server address. */
    public Main() {
        this(null);
    }

    /**
     * Creates a client.
     *
     * @param host server address shown in the nickname screen, or {@code null} for {@code localhost}
     */
    public Main(String host) {
        this.host = host;
    }

    @Override
    public void create() {
        skin = UiSkin.create();
        showScreen(new NameScreen(this, skin, host == null ? DEFAULT_HOST : host));
    }

    /** Shows a screen and frees the previous one (libGDX does not). */
    public void showScreen(Screen next) {
        Screen previous = getScreen();
        setScreen(next);
        if (previous != null) {
            previous.dispose();
        }
    }

    /**
     * Opens a network session, closing the previous one.
     *
     * @param address address of the server
     * @throws IOException if the server cannot be reached
     */
    public GameSession connect(String address) throws IOException {
        closeSession();
        session = new RemoteSession(address);
        return session;
    }

    /**
     * Starts a solo game on the default map, directly in the match.
     *
     * @return the session; its {@code playerId()} is -1 if the nickname was refused
     */
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

    /** Shows the match screen on a map, with the current session. */
    public void play(String map) {
        showScreen(new GameScreen(this, skin, session, ArenaLoader.loadResource(map), inputSequence));
    }

    /** Closes the session and restarts the input numbering. */
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
