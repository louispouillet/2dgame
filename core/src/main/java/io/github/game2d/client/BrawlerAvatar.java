package io.github.game2d.client;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import io.github.game2d.protocol.BrawlerState;

public interface BrawlerAvatar {
    void update(BrawlerState state, float frameTime);

    void render(ModelBatch batch, Environment environment);
}
