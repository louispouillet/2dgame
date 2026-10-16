package io.github.game2d.client;

import io.github.game2d.model.brawler.BrawlerType;
import java.util.Map;

/** The bullet style of each brawler type: the default one, unless its images have another shape. */
public final class BulletStyles {
    /** Fits Colt's images (512x128, head on the right): bullets of a burst are 0.9 tiles apart. */
    public static final BulletStyle DEFAULT = new BulletStyle(0.9f, 0.225f, 0.8f);

    // A type whose images have another shape gets its own line, e.g. BrawlerType.SHELLY, new BulletStyle(0.6f, 0.3f, 0.7f)
    private static final Map<BrawlerType, BulletStyle> STYLES = Map.of();

    private BulletStyles() {
    }

    public static BulletStyle of(BrawlerType type) {
        return STYLES.getOrDefault(type, DEFAULT);
    }
}
