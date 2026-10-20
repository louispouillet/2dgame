package io.github.game2d.client;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.LobbyState;
import io.github.game2d.protocol.WorldSnapshot;

public class GameScreen extends ScreenAdapter {
    private final Main main;
    private final Skin skin;
    private final GameSession session;
    private final Arena arena;
    private final WorldRenderer3D renderer;
    private final InputReader inputReader;
    private final ClientGame game;
    private final PlayerRoster roster;
    private final MatchHud matchHud;
    private final HitFlash hitFlash = new HitFlash();
    private final HitOverlay hitOverlay = new HitOverlay();

    public GameScreen(Main main, Skin skin, GameSession session, Arena arena, InputSequence inputSequence) {
        this.main = main;
        this.skin = skin;
        this.session = session;
        this.arena = arena;
        roster = PlayerRoster.from(session.channel().latestLobby(), session.playerId());
        renderer = new WorldRenderer3D(arena, roster);
        inputReader = new InputReader(renderer, inputSequence);
        game = new ClientGame(arena, session);
        matchHud = new MatchHud(skin);
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
    }

    @Override
    public void render(float delta) {
        LobbyState lobby = session.channel().latestLobby();
        if (lobby != null && !lobby.started()) {
            main.showScreen(new LobbyScreen(main, skin, session));
            return;
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        WorldSnapshot snapshot = game.update(now, inputReader);
        if (snapshot != null) {
            renderer.render(snapshot, arena);
            updateHitFlash(snapshot, delta);
            hitOverlay.draw(hitFlash.intensity());
            matchHud.draw(new MatchView(snapshot.match(), roster.myTeam()),
                MatchView.watchingText(snapshot.followedId(), roster));
        }
    }

    /** Only my own living brawler makes my screen flash: not the ally I watch, nor the hit that kills me. */
    private void updateHitFlash(WorldSnapshot snapshot, float frameTime) {
        BrawlerState me = snapshot.findBrawler(session.playerId());
        if (me == null) {
            hitFlash.reset();
        } else {
            hitFlash.update(me.health(), frameTime);
        }
    }

    @Override
    public void dispose() {
        renderer.dispose();
        matchHud.dispose();
        hitOverlay.dispose();
    }
}
