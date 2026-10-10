package io.github.game2d.model.weapon;

import java.util.List;

/** La forme d'une attaque : quelles balles partent pour une munition, et quand. */
public interface AttackPattern {
    List<Shot> shots();
}
