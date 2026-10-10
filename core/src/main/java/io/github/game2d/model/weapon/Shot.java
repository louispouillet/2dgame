package io.github.game2d.model.weapon;

/**
 * Une balle d'une attaque, relative à la visée : quand elle part après le tir (secondes),
 * son décalage sur le côté (positif = à droite) et son angle (degrés, positif = vers la gauche).
 */
public record Shot(float delay, float sideOffset, float angle) {
}
