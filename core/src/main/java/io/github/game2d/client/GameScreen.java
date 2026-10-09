package io.github.game2d.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.protocol.WorldSnapshot;

public class GameScreen extends ScreenAdapter {
    private final GameSession session;
    private final Arena arena;
    private final WorldRenderer renderer;
    private final WorldRenderer3D renderer3d;
    private final InputReader inputReader;
    private final ClientGame game;
    private GameView view;

    public GameScreen(GameSession session, Arena arena) {
        this.session = session;
        this.arena = arena;
        PlayerNames names = PlayerNames.from(session.channel().latestLobby());
        renderer = new WorldRenderer(arena.getWidth(), arena.getHeight(), names);
        renderer3d = new WorldRenderer3D(arena, names);
        view = renderer;
        inputReader = new InputReader(() -> view);
        game = new ClientGame(arena, session);
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
        renderer3d.resize(width, height);
    }

    @Override
    public void render(float delta) {
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
        renderer.dispose();
        renderer3d.dispose();
    }
}
