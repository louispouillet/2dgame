package io.github.game2d.client;

import com.badlogic.gdx.ScreenAdapter;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.protocol.WorldSnapshot;

public class GameScreen extends ScreenAdapter {
    private final GameSession session;
    private final Arena arena;
    private final WorldRenderer3D renderer;
    private final InputReader inputReader;
    private final ClientGame game;

    public GameScreen(GameSession session, Arena arena) {
        this.session = session;
        this.arena = arena;
        renderer = new WorldRenderer3D(arena, PlayerRoster.from(session.channel().latestLobby(), session.playerId()));
        inputReader = new InputReader(renderer);
        game = new ClientGame(arena, session);
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
    }

    @Override
    public void render(float delta) {
        double now = System.nanoTime() / 1_000_000_000.0;
        WorldSnapshot snapshot = game.update(now, inputReader);
        if (snapshot != null) {
            renderer.render(snapshot, arena, session.playerId());
        }
    }

    @Override
    public void dispose() {
        renderer.dispose();
    }
}
