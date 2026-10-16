package io.github.game2d.client;

import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.game2d.model.brawler.BrawlerType;
import org.junit.jupiter.api.Test;

class BulletStylesTest {
    @Test
    void aTypeWithoutItsOwnStyleGetsTheDefault() {
        assertSame(BulletStyles.DEFAULT, BulletStyles.of(BrawlerType.COLT));
    }
}
