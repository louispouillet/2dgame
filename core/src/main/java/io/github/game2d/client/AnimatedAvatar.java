package io.github.game2d.client;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import io.github.game2d.protocol.BrawlerState;

/**
 * A brawler drawn from a glTF model. It picks its own clips from the state it receives:
 * idle or run for the body, the same for the upper body, and a shoot clip on top at each new shot.
 */
public class AnimatedAvatar implements BrawlerAvatar {
    private static final String IDLE = "Idle";
    private static final String RUN = "Run";
    private static final String IDLE_UPPER = "IdleUpper";
    private static final String RUN_UPPER = "RunUpper";
    private static final String SHOOT = "Shoot";

    private static final float BODY_BLEND = 0.2f;
    private static final float SHOT_BLEND = 0.05f;
    private static final float UPPER_BLEND = 0.2f;
    private static final int LOOP = -1;

    private final ModelInstance instance;
    private final AnimationController body;
    private final AnimationController upper;
    private String bodyClip;
    private String upperClip;
    private int lastShotsFired = -1;

    /** Creates an avatar from the model of a brawler type. Each avatar has its own animation state. */
    public AnimatedAvatar(Model model) {
        this.instance = new ModelInstance(model);
        this.body = new AnimationController(instance);
        this.upper = new AnimationController(instance);
    }

    @Override
    public void update(BrawlerState state, float frameTime) {
        boolean newShot = lastShotsFired >= 0 && state.shotsFired() > lastShotsFired;
        lastShotsFired = state.shotsFired();

        playBody(state.moving());
        playUpper(state.moving(), newShot);

        float scale = state.radius() / AvatarFactory.MODEL_RADIUS;
        float rotation = state.facing() * MathUtils.radiansToDegrees + 90f;
        instance.transform.setToTranslation(state.x(), 0f, -state.y())
            .rotate(Vector3.Y, rotation)
            .scale(scale, scale, scale);

        body.update(frameTime);
        upper.update(frameTime);
    }

        /** Switches the body clip (idle or run) with a short blend, only when it changes. */
        private void playBody(boolean moving) {
        String clip = moving ? RUN : IDLE;
        if (!clip.equals(bodyClip)) {
            bodyClip = clip;
            body.animate(clip, LOOP, 1f, null, BODY_BLEND);
        }
    }

    /** Switches the upper body clip, and plays the shoot clip once on a new shot. */
    private void playUpper(boolean moving, boolean newShot) {
        String clip = moving ? RUN_UPPER : IDLE_UPPER;
        if (!clip.equals(upperClip)) {
            upperClip = clip;
            upper.animate(clip, LOOP, 1f, null, UPPER_BLEND);
        }
        if (newShot) {
            upper.action(SHOOT, 1, 1f, null, SHOT_BLEND);
        }
    }

    @Override
    public void render(ModelBatch batch, Environment environment) {
        batch.render(instance, environment);
    }
}
