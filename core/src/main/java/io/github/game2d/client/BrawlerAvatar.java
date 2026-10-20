package io.github.game2d.client;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import io.github.game2d.protocol.BrawlerState;

/** The 3D look of one brawler: it follows its state and draws itself. */
public interface BrawlerAvatar {
    /**
     * Moves, turns and animates the avatar.
     *
     * @param state     the (interpolated) state of the brawler
     * @param frameTime seconds since the last frame
     */
    void update(BrawlerState state, float frameTime);

    /** Draws the avatar. */
    void render(ModelBatch batch, Environment environment);
}
