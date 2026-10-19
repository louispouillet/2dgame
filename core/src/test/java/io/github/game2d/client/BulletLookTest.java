package io.github.game2d.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.game2d.model.Team;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.protocol.LobbyPlayerState;
import io.github.game2d.protocol.LobbyState;
import io.github.game2d.protocol.ProjectileState;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BulletLookTest {
    private static final LobbyState LOBBY = new LobbyState(
        List.of(
            new LobbyPlayerState(1, "Louis", Team.BLUE, BrawlerType.COLT),
            new LobbyPlayerState(2, "Lilian", Team.RED, BrawlerType.COLT),
            new LobbyPlayerState(3, "Tom", Team.BLUE, BrawlerType.COLT)),
        1, "maps/arena2.txt", List.of("maps/arena2.txt"), true, true);
    private static final PlayerRoster ROSTER = PlayerRoster.from(LOBBY, 1);
    private static final Set<BrawlerType> ONLY_COLT = EnumSet.of(BrawlerType.COLT);

    private static ProjectileState firedBy(int ownerId, BrawlerType shooter) {
        return new ProjectileState(10, 5f, 5f, 0.1f, ownerId, 1f, 0f, shooter);
    }

    @Test
    void myBulletsAndThoseOfMyAlliesAreBlue() {
        assertEquals(new BulletLook.Textured(BrawlerType.COLT, true),
            BulletLook.of(firedBy(1, BrawlerType.COLT), ROSTER, ONLY_COLT));
        assertEquals(new BulletLook.Textured(BrawlerType.COLT, true),
            BulletLook.of(firedBy(3, BrawlerType.COLT), ROSTER, ONLY_COLT));
    }

    @Test
    void enemyBulletsAreRed() {
        assertEquals(new BulletLook.Textured(BrawlerType.COLT, false),
            BulletLook.of(firedBy(2, BrawlerType.COLT), ROSTER, ONLY_COLT));
    }

    @Test
    void aTypeWithoutImagesKeepsTheSphere() {
        assertEquals(new BulletLook.Sphere(), BulletLook.of(firedBy(2, BrawlerType.SHELLY), ROSTER, ONLY_COLT));
    }

    @Test
    void aTypeWhoseImagesWereFoundGetsTexturedBullets() {
        Set<BrawlerType> coltAndShelly = EnumSet.of(BrawlerType.COLT, BrawlerType.SHELLY);

        assertEquals(new BulletLook.Textured(BrawlerType.SHELLY, false),
            BulletLook.of(firedBy(2, BrawlerType.SHELLY), ROSTER, coltAndShelly));
    }

    @Test
    void anUnknownShooterKeepsTheSphere() {
        assertEquals(new BulletLook.Sphere(), BulletLook.of(firedBy(2, null), ROSTER, ONLY_COLT));
    }

    @Test
    void imagesFollowTheNamingConvention() {
        assertEquals("textures/projectiles/colt_bullet_blue.png", new BulletLook.Textured(BrawlerType.COLT, true).texturePath());
        assertEquals("textures/projectiles/shelly_bullet_red.png", new BulletLook.Textured(BrawlerType.SHELLY, false).texturePath());
    }

    @Test
    void coltHasItsTwoBulletImages() {
        for (boolean ally : new boolean[] {true, false}) {
            String path = new BulletLook.Textured(BrawlerType.COLT, ally).texturePath();
            assertNotNull(getClass().getClassLoader().getResource(path), path);
        }
    }
}
