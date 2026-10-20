package io.github.game2d.model.weapon;

import java.util.List;

/** The shape of an attack: which bullets one ammo fires, and when. */
public interface AttackPattern {
    /** Returns the bullets fired by one attack. */
    List<Shot> shots();
}
