package io.github.game2d.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

/** Red screen edges, fading towards the center, drawn over the 3D view when I am hit. */
public class HitOverlay {
    private static final float MAX_ALPHA = 0.4f;
    // Width of each red band, as a share of the smaller side of the screen
    private static final float BAND_RATIO = 0.1f;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final Matrix4 projection = new Matrix4();
    private final Color edge = new Color();
    private final Color center = new Color(1f, 0f, 0f, 0f);

    /**
     * Draws the red edges.
     *
     * @param intensity strength from 0 (nothing drawn) to 1
     */
    public void draw(float intensity) {
        if (intensity <= 0f) {
            return;
        }
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        float band = Math.min(width, height) * BAND_RATIO;
        edge.set(1f, 0f, 0f, MAX_ALPHA * intensity);
        projection.setToOrtho2D(0f, 0f, width, height);
        shapes.setProjectionMatrix(projection);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Corner colors: bottom left, bottom right, top right, top left
        shapes.rect(0f, 0f, band, height, edge, center, center, edge);
        shapes.rect(width - band, 0f, band, height, center, edge, edge, center);
        shapes.rect(0f, 0f, width, band, edge, edge, center, center);
        shapes.rect(0f, height - band, width, band, center, center, edge, edge);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /** Frees the graphics resources. */
    public void dispose() {
        shapes.dispose();
    }
}
