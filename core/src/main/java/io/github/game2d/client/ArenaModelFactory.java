package io.github.game2d.client;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.TileType;

public final class ArenaModelFactory {
    static final float OUTSIDE_MARGIN = 3f;

    private static final Color FLOOR_COLOR = new Color(0.86f, 0.74f, 0.52f, 1f);
    private static final Color OUTSIDE_COLOR = new Color(0.62f, 0.52f, 0.36f, 1f);
    private static final float OUTSIDE_LEVEL = -0.01f;
    private static final Color WALL_COLOR = new Color(0.55f, 0.38f, 0.25f, 1f);
    private static final Color BUSH_COLOR = new Color(0.25f, 0.60f, 0.30f, 1f);
    private static final Color WATER_COLOR = new Color(0.20f, 0.45f, 0.85f, 1f);
    private static final float WALL_HEIGHT = 1f;
    private static final float BUSH_HEIGHT = 1f;
    private static final float WATER_LEVEL = 0.01f;
    private static final long ATTRIBUTES = Usage.Position | Usage.Normal;

    private ArenaModelFactory() {
    }

    public static Model create(Arena arena) {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        addOutside(builder, arena);
        addFloor(builder, arena);
        addTiles(builder, arena, TileType.WALL, WALL_COLOR,
            (part, col, row) -> addBlock(part, col, row, WALL_HEIGHT));
        addTiles(builder, arena, TileType.BUSH, BUSH_COLOR,
            (part, col, row) -> addBlock(part, col, row, BUSH_HEIGHT));
        addTiles(builder, arena, TileType.WATER, WATER_COLOR, ArenaModelFactory::addWater);
        return builder.end();
    }

    private static void addOutside(ModelBuilder builder, Arena arena) {
        float left = -OUTSIDE_MARGIN;
        float right = arena.getWidth() + OUTSIDE_MARGIN;
        float near = OUTSIDE_MARGIN;
        float far = -arena.getHeight() - OUTSIDE_MARGIN;
        MeshPartBuilder outside = builder.part("outside", GL20.GL_TRIANGLES, ATTRIBUTES, material(OUTSIDE_COLOR));
        outside.rect(
            left, OUTSIDE_LEVEL, near,
            right, OUTSIDE_LEVEL, near,
            right, OUTSIDE_LEVEL, far,
            left, OUTSIDE_LEVEL, far,
            0f, 1f, 0f);
    }

    private static void addFloor(ModelBuilder builder, Arena arena) {
        float width = arena.getWidth();
        float depth = arena.getHeight();
        MeshPartBuilder floor = builder.part("floor", GL20.GL_TRIANGLES, ATTRIBUTES, material(FLOOR_COLOR));
        floor.rect(
            0f, 0f, 0f,
            width, 0f, 0f,
            width, 0f, -depth,
            0f, 0f, -depth,
            0f, 1f, 0f);
    }

    private static void addTiles(ModelBuilder builder, Arena arena, TileType type, Color color, TileShape shape) {
        MeshPartBuilder part = null;
        for (int row = 0; row < arena.getHeight(); row++) {
            for (int col = 0; col < arena.getWidth(); col++) {
                if (arena.getTile(col, row) == type) {
                    if (part == null) {
                        part = builder.part(type.name(), GL20.GL_TRIANGLES, ATTRIBUTES, material(color));
                    }
                    shape.build(part, col, row);
                }
            }
        }
    }

    private static void addBlock(MeshPartBuilder part, int col, int row, float height) {
        BoxShapeBuilder.build(part, col + 0.5f, height / 2, -(row + 0.5f), 1f, height, 1f);
    }

    private static void addWater(MeshPartBuilder part, int col, int row) {
        part.rect(
            col, WATER_LEVEL, -row,
            col + 1f, WATER_LEVEL, -row,
            col + 1f, WATER_LEVEL, -(row + 1f),
            col, WATER_LEVEL, -(row + 1f),
            0f, 1f, 0f);
    }

    private static Material material(Color color) {
        return new Material(ColorAttribute.createDiffuse(color));
    }

    @FunctionalInterface
    private interface TileShape {
        void build(MeshPartBuilder part, int col, int row);
    }
}
