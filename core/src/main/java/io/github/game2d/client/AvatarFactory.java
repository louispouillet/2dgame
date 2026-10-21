package io.github.game2d.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Disposable;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.protocol.BrawlerState;
import java.util.EnumMap;
import java.util.Map;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

/**
 * Loads the 3D models once and creates one {@link BrawlerAvatar} per brawler.
 * A brawler type without model gets a capsule.
 */
public class AvatarFactory implements Disposable {
    /** Radius of the models, in model units: avatars are scaled to the radius of the hitbox. */
    public static final float MODEL_RADIUS = 0.5f;
    /** Height of the models, in model units. */
    public static final float MODEL_HEIGHT = 2f;

    private static final Map<BrawlerType, String> MODELS = Map.of(
        BrawlerType.SHELLY, "models/shelly.glb",
        BrawlerType.COLT, "models/colt.glb"
    );

    private final Model capsule;
    private final Map<BrawlerType, SceneAsset> assets = new EnumMap<>(BrawlerType.class);

    /** Loads the models. Needs the OpenGL context. */
    public AvatarFactory() {
        capsule = new ModelBuilder().createCapsule(MODEL_RADIUS, MODEL_HEIGHT, 16, new Material(), Usage.Position | Usage.Normal);
        capsule.nodes.first().translation.set(0f, MODEL_HEIGHT / 2f, 0f);
        capsule.calculateTransforms();

        MODELS.forEach((type, path) -> {
            SceneAsset asset = new GLBLoader().load(Gdx.files.internal(path));
            useBaseColorAsDiffuse(asset.scene.model);
            assets.put(type, asset);
        });
    }

    /** Creates the avatar of a brawler, according to its type. */
    public BrawlerAvatar create(BrawlerState state) {
        if (!MODELS.containsKey(state.type())) {
            return new CapsuleAvatar(capsule);
        }
        return new AnimatedAvatar(assets.get(state.type()).scene.model);
    }

    /** gdx-gltf stores the texture in a PBR attribute, which libGDX's default shader ignores. */
    private static void useBaseColorAsDiffuse(Model model) {
        for (Material material : model.materials) {
            PBRTextureAttribute baseColor = material.get(PBRTextureAttribute.class, PBRTextureAttribute.BaseColorTexture);
            if (baseColor != null) {
                material.set(new TextureAttribute(TextureAttribute.Diffuse, baseColor.textureDescription));
            }
        }
    }

    @Override
    public void dispose() {
        capsule.dispose();
        assets.values().forEach(SceneAsset::dispose);
    }
}
