package io.github.game2d.client;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import io.github.game2d.protocol.BrawlerState;

/** A white capsule, for brawlers that have no 3D model yet. */
public class CapsuleAvatar implements BrawlerAvatar {
    private final ModelInstance instance;

    /** @param capsule capsule of radius {@link AvatarFactory#MODEL_RADIUS}, origin at the feet */
    public CapsuleAvatar(Model capsule) {
        instance = new ModelInstance(capsule);
    }

    @Override
    public void update(BrawlerState state, float frameTime) {
        float scale = state.radius() / AvatarFactory.MODEL_RADIUS;
        instance.transform.setToTranslation(state.x(), 0f, -state.y()).scale(scale, scale, scale);
    }

    @Override
    public void render(ModelBatch batch, Environment environment) {
        batch.render(instance, environment);
    }
}
