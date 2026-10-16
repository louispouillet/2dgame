package io.github.game2d.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.TileType;
import java.util.ArrayList;
import java.util.List;

public final class ArenaModelFactory {
    static final float OUTSIDE_MARGIN = 12f;

    private static final String TEXTURE_FOLDER = "textures/arena/";
    private static final float OUTSIDE_LEVEL = -0.01f;
    private static final float FLOOR_LEVEL = 0f;
    private static final float WATER_LEVEL = 0.01f;
    private static final float WALL_HEIGHT = 1f;
    private static final float BUSH_HEIGHT = 1f;
    private static final long ATTRIBUTES = Usage.Position | Usage.Normal | Usage.TextureCoordinates;

    private ArenaModelFactory() {
    }

    public static Model create(Arena arena) {
        List<Texture> textures = new ArrayList<>();
        float width = arena.getWidth();
        float depth = arena.getHeight();

        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        MeshPartBuilder outside = part(builder, "outside", load(textures, "outside.png"));
        outside.setUVRange(0f, 0f, width + 2 * OUTSIDE_MARGIN, depth + 2 * OUTSIDE_MARGIN);
        addFlat(outside, -OUTSIDE_MARGIN, -OUTSIDE_MARGIN, width + OUTSIDE_MARGIN, depth + OUTSIDE_MARGIN, OUTSIDE_LEVEL);

        MeshPartBuilder floor = part(builder, "floor", load(textures, "floor.png"));
        floor.setUVRange(0f, 0f, width, depth);
        addFlat(floor, 0f, 0f, width, depth, FLOOR_LEVEL);

        addTiles(builder, arena, TileType.WALL, "wall_top", load(textures, "wall_top.png"),
            (part, col, row) -> addTop(part, col, row, WALL_HEIGHT));
        addTiles(builder, arena, TileType.WALL, "wall_side", load(textures, "wall_side.png"),
            (part, col, row) -> addSides(part, col, row, WALL_HEIGHT));
        addTiles(builder, arena, TileType.WATER, "water", load(textures, "water.png"),
            (part, col, row) -> addFlat(part, col, row, col + 1f, row + 1f, WATER_LEVEL));

        return finish(builder, textures);
    }

    public static Model createBush() {
        List<Texture> textures = new ArrayList<>();
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        addTop(part(builder, "bush_top", load(textures, "bush_top.png")), 0, 0, BUSH_HEIGHT);
        addSides(part(builder, "bush_side", load(textures, "bush_side.png")), 0, 0, BUSH_HEIGHT);
        return finish(builder, textures);
    }

    private static Model finish(ModelBuilder builder, List<Texture> textures) {
        Model model = builder.end();
        for (Texture texture : textures) {
            model.manageDisposable(texture);
        }
        return model;
    }

    private static Texture load(List<Texture> textures, String file) {
        Texture texture = new Texture(Gdx.files.internal(TEXTURE_FOLDER + file), true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        textures.add(texture);
        return texture;
    }

    private static MeshPartBuilder part(ModelBuilder builder, String name, Texture texture) {
        return builder.part(name, GL20.GL_TRIANGLES, ATTRIBUTES, new Material(TextureAttribute.createDiffuse(texture)));
    }

    private static void addTiles(ModelBuilder builder, Arena arena, TileType type, String name, Texture texture, TileShape shape) {
        MeshPartBuilder part = null;
        for (int row = 0; row < arena.getHeight(); row++) {
            for (int col = 0; col < arena.getWidth(); col++) {
                if (arena.getTile(col, row) == type) {
                    if (part == null) {
                        part = part(builder, name, texture);
                    }
                    shape.build(part, col, row);
                }
            }
        }
    }

    private static void addFlat(MeshPartBuilder part, float left, float bottom, float right, float top, float level) {
        part.rect(
            left, level, -bottom,
            right, level, -bottom,
            right, level, -top,
            left, level, -top,
            0f, 1f, 0f
        );
    }

    private static void addTop(MeshPartBuilder part, int col, int row, float height) {
        addFlat(part, col, row, col + 1f, row + 1f, height);
    }

    private static void addSides(MeshPartBuilder part, int col, int row, float height) {
        float left = col;
        float right = col + 1f;
        float near = -row;
        float far = -(row + 1f);
        part.rect(left, 0f, near, right, 0f, near, right, height, near, left, height, near, 0f, 0f, 1f);
        part.rect(right, 0f, far, left, 0f, far, left, height, far, right, height, far, 0f, 0f, -1f);
        part.rect(right, 0f, near, right, 0f, far, right, height, far, right, height, near, 1f, 0f, 0f);
        part.rect(left, 0f, far, left, 0f, near, left, height, near, left, height, far, -1f, 0f, 0f);
    }

    @FunctionalInterface
    private interface TileShape {
        void build(MeshPartBuilder part, int col, int row);
    }
}
