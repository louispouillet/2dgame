package io.github.game2d.protocol;

import java.util.List;

public record WorldSnapshot(
    int tick,
    List<BrawlerState> brawlers,
    List<ProjectileState> projectiles
) {}
