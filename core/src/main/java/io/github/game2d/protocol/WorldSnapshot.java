package io.github.game2d.protocol;

import java.util.List;

public record WorldSnapshot(
    List<BrawlerState> brawlers,
    List<ProjectileState> projectiles
) {}
