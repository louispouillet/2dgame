package io.github.game2d.client;

import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.protocol.ProjectileState;
import java.util.Locale;
import java.util.Set;

/** What to draw for a projectile: the textured bullet of the brawler who fired it, or the default sphere. */
public sealed interface BulletLook {
    String TEXTURE_FOLDER = "textures/projectiles/";

    /** The shooter is unknown, or its brawler type has no bullet images. */
    record Sphere() implements BulletLook {
    }

    /** Bullets of my team are blue, the others red. */
    record Textured(BrawlerType type, boolean ally) implements BulletLook {
        /** Naming convention: textures/projectiles/<type>_bullet_<blue|red>.png, e.g. colt_bullet_blue.png. */
        public String texturePath() {
            return TEXTURE_FOLDER + type.name().toLowerCase(Locale.ROOT) + "_bullet_" + (ally ? "blue" : "red") + ".png";
        }
    }

    /** texturedTypes: the brawler types whose bullet images were found. */
    static BulletLook of(ProjectileState projectile, PlayerRoster roster, Set<BrawlerType> texturedTypes) {
        BrawlerType shooter = projectile.shooter();
        if (shooter == null || !texturedTypes.contains(shooter)) {
            return new Sphere();
        }
        return new Textured(shooter, roster.isAlly(projectile.ownerId()));
    }
}
