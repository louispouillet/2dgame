package io.github.game2d.model.collision;

public interface Obstacles {
    boolean blocksMovement(float centerX, float centerY, float radius);

    boolean blocksShots(float centerX, float centerY, float radius);
}
