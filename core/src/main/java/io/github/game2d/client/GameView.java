package io.github.game2d.client;

import com.badlogic.gdx.math.Vector2;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.protocol.WorldSnapshot;

public interface GameView {
    void resize(int width, int height);

    void render(WorldSnapshot snapshot, Arena arena, int playerId);

    Vector2 screenToWorld(int screenX, int screenY);

    void dispose();
}
