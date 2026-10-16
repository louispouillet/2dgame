package io.github.game2d.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Disposable;
import io.github.game2d.model.brawler.BrawlerType;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Looks for the bullet images of every brawler type (see BulletLook.Textured.texturePath) and builds
 * a flat textured model for each one found. A type with a missing image keeps the sphere.
 */
public class BulletModels implements Disposable {
    private static final long ATTRIBUTES = Usage.Position | Usage.Normal | Usage.TextureCoordinates;

    private final Set<BrawlerType> texturedTypes = EnumSet.noneOf(BrawlerType.class);
    private final Map<BulletLook.Textured, Model> models = new HashMap<>();
    private final Map<BulletLook.Textured, ModelInstance> instances = new HashMap<>();

    public BulletModels() {
        for (BrawlerType type : BrawlerType.values()) {
            BulletLook.Textured ally = new BulletLook.Textured(type, true);
            BulletLook.Textured enemy = new BulletLook.Textured(type, false);
            if (exists(ally) && exists(enemy)) {
                BulletStyle style = BulletStyles.of(type);
                add(ally, style);
                add(enemy, style);
                texturedTypes.add(type);
            }
        }
    }

    private static boolean exists(BulletLook.Textured look) {
        return Gdx.files.internal(look.texturePath()).exists();
    }

    /** A rectangle lying on the ground, pointing to +x, with the head of the bullet at the origin. */
    private void add(BulletLook.Textured look, BulletStyle style) {
        Texture texture = new Texture(Gdx.files.internal(look.texturePath()), true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        // Transparent background; bullets do not hide each other where their images overlap
        Material material = new Material(TextureAttribute.createDiffuse(texture), new BlendingAttribute(),
            new DepthTestAttribute(GL20.GL_LEQUAL, false));

        float tail = -style.headPosition() * style.length();
        float front = (1f - style.headPosition()) * style.length();
        float side = style.width() / 2f;
        Model model = new ModelBuilder().createRect(
            tail, 0f, side,
            front, 0f, side,
            front, 0f, -side,
            tail, 0f, -side,
            0f, 1f, 0f, material, ATTRIBUTES);
        model.manageDisposable(texture);
        models.put(look, model);
        instances.put(look, new ModelInstance(model));
    }

    public Set<BrawlerType> texturedTypes() {
        return Collections.unmodifiableSet(texturedTypes);
    }

    /** One instance per look, reused for every bullet: only its transform changes. */
    public ModelInstance instance(BulletLook.Textured look) {
        return instances.get(look);
    }

    @Override
    public void dispose() {
        models.values().forEach(Model::dispose);
    }
}
