package io.github.game2d.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.protocol.ProjectileState;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Draws bullets as textured quads along their direction, tilted to face the camera so they look airborne.
 * The quad keeps the proportions of its texture, so images of any aspect ratio are drawn undistorted.
 * The texture of a bullet is {@code textures/<type>_bullet_blue.png} for me and my allies and
 * {@code textures/<type>_bullet_red.png} for my enemies: a brawler without both files has no sprite
 * ({@link #hasSprite}) and the caller draws a sphere instead.
 */
public class BulletSprites {
    private static final float HEIGHT = 0.5f;
    private static final float TILT = 35f;
    private static final float HEAD_POSITION = 0.8f;
    private static final float WIDTH_IN_RADII = 4f;
    private static final float PULSE_AMOUNT = 0.10f;
    private static final float PULSE_SPEED = 10f;
    private static final float PHASE_PER_ID = 1.7f;

    private record Sprite(Texture texture, Model model, ModelInstance instance, float aspect) {}
    private final Map<String, Sprite> sprites = new HashMap<>();
    private final Map<BrawlerType, Boolean> hasSprite = new EnumMap<>(BrawlerType.class);
    private float time;

    /** Advances the animation clock; a bullet pulses with a phase of its own, so the bullets of a volley are out of step. */
    public void update(float frameTime) {
        time += frameTime;
    }

    /** Whether both team images of this type exist; a type of {@code null} (shooter gone) has none. */
    public boolean hasSprite(BrawlerType type) {
        if (type == null) {
            return false;
        }
        return hasSprite.computeIfAbsent(type,
            t -> Gdx.files.internal(fileName(t, true)).exists() && Gdx.files.internal(fileName(t, false)).exists());
    }

    /** Draws the sprite of a projectile whose type {@link #hasSprite has one}. */
    public void render(ModelBatch modelBatch, Environment environment, ProjectileState projectile, boolean ally) {
        String path = fileName(projectile.ownerType(), ally);
        Sprite sprite = sprites.get(path);
        if (sprite == null) {
            sprite = load(path);
            sprites.put(path, sprite);
        }

        float width = projectile.radius() * WIDTH_IN_RADII;
        float heading = MathUtils.atan2(projectile.dirY(), projectile.dirX()) * MathUtils.radiansToDegrees;
        float pulse = 1f + PULSE_AMOUNT * MathUtils.sin(time * PULSE_SPEED + projectile.id() * PHASE_PER_ID);
        float length = width * sprite.aspect() * pulse;
        float thickness = width * sprite.aspect() * (2f - pulse);
        sprite.instance().transform.setToTranslation(projectile.x(), HEIGHT, -projectile.y())
            .rotate(Vector3.X, TILT)
            .rotate(Vector3.Y, heading)
            .scale(length, 1f, thickness);
        modelBatch.render(sprite.instance(), environment);
    }

    /** Frees the textures and the models. */
    public void dispose() {
        for (Sprite sprite : sprites.values()) {
            sprite.model().dispose();
            sprite.texture().dispose();
        }
        sprites.clear();
    }

    /** Returns the image path of a brawler type: blue for allies, red for enemies. */
    private static String fileName(BrawlerType type, boolean ally) {
        String team = ally ? "blue" : "red";
        return "textures/" + type.name().toLowerCase() + "_bullet_" + team + ".png";
    }

    /** Loads the image of a bullet and builds its quad. */
    private static Sprite load(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        float aspect = texture.getWidth() / (float) texture.getHeight();
        Model model = createQuad(texture, aspect);
        return new Sprite(texture, model, new ModelInstance(model), aspect);
    }

    /** Quad of length 1 along x and the texture's proportions, the bullet head at the origin and the trail towards -x. */
    private static Model createQuad(Texture texture, float aspect) {
        Material material = new Material(
            TextureAttribute.createDiffuse(texture),
            new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        );
        float back = -HEAD_POSITION;
        float front = 1f - HEAD_POSITION;
        float halfWidth = 0.5f / aspect;

        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        MeshPartBuilder part = builder.part("bullet", GL20.GL_TRIANGLES,
            Usage.Position | Usage.Normal | Usage.TextureCoordinates, material);
        part.rect(
            back, 0f, halfWidth,
            front, 0f, halfWidth,
            front, 0f, -halfWidth,
            back, 0f, -halfWidth,
            0f, 1f, 0f
        );
        return builder.end();
    }
}
