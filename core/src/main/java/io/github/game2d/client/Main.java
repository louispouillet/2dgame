package io.github.game2d.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.utils.GdxRuntimeException;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.ArenaLoader;
import java.io.IOException;

public class Main extends Game {
    private static final String SOLO_NAME = "Joueur";

    private final String host;
    private GameSession session;

    public Main() {
        this(null);
    }

    public Main(String host) {
        this.host = host;
    }

    @Override
    public void create() {
        Arena arena = ArenaLoader.loadResource(ArenaLoader.DEFAULT_MAP);
        session = openSession(arena);
        setScreen(new GameScreen(session, arena));
    }

    private GameSession openSession(Arena arena) {
        if (host == null) {
            LocalSession solo = new LocalSession(arena);
            solo.join(SOLO_NAME);
            solo.start();
            return solo;
        }
        try {
            return new RemoteSession(host);
        } catch (IOException e) {
            throw new GdxRuntimeException("Cannot connect to " + host, e);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (getScreen() != null) {
            getScreen().dispose();
        }
        if (session != null) {
            session.close();
        }
    }
}
