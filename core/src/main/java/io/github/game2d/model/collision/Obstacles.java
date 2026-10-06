package io.github.game2d.model.collision;

public interface Obstacles {
    Obstacles NONE = new Obstacles() {
        @Override
        public boolean blocksMovement(float centerX, float centerY, float radius) {
            return false;
        }

        @Override
        public boolean blocksShots(float centerX, float centerY, float radius) {
            return false;
        }
    };

    boolean blocksMovement(float centerX, float centerY, float radius);

    boolean blocksShots(float centerX, float centerY, float radius);
}
