package io.github.game2d.model.weapon;

/**
 * One bullet of an attack, relative to the aim: when it leaves after the shot (seconds),
 * its side offset (positive = right) and its angle (degrees, positive = to the left).
 */
public record Shot(float delay, float sideOffset, float angle) {
}
