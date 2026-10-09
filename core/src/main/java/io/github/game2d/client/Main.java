package io.github.game2d.client;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.GdxRuntimeException;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.ArenaLoader;
import io.github.game2d.protocol.WorldSnapshot;
import java.io.IOException;

public class Main extends ApplicationAdapter {
    private static final String SOLO_NAME = "Joueur";

    private final String host;
    private Arena arena;
    private WorldRenderer renderer;
    private WorldRenderer3D renderer3d;
    private GameView view;
    private InputReader inputReader;
    private GameSession session;
    private ClientGame game;

    public Main() {
        this(null);
    }

    public Main(String host) {
        this.host = host;
    }

    @Override
    public void create() {
        arena = ArenaLoader.loadResource(ArenaLoader.DEFAULT_MAP);
        renderer = new WorldRenderer(arena.getWidth(), arena.getHeight());
        renderer3d = new WorldRenderer3D(arena);
        view = renderer;
        inputReader = new InputReader(() -> view);
        session = openSession();
        game = new ClientGame(arena, session);
    }

    private GameSession openSession() {
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
    public void resize(int width, int height) {
        renderer.resize(width, height);
        renderer3d.resize(width, height);
    }

    @Override
    public void render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.V)) {
            view = view == renderer ? renderer3d : renderer;
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        WorldSnapshot snapshot = game.update(now, inputReader);
        if (snapshot != null) {
            view.render(snapshot, arena, session.playerId());
        }
    }

    @Override
    public void dispose() {
        if (session != null) {
            session.close();
        }
        renderer.dispose();
        renderer3d.dispose();
    }
}
