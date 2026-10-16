package io.github.game2d.client;

/**
 * Size of a textured bullet on the ground, in tiles.
 * headPosition: where the head is drawn along the image, from 0 (left edge) to 1 (right edge);
 * this point is placed on the projectile, the rest of the image trails behind it.
 */
public record BulletStyle(float length, float width, float headPosition) {
}
