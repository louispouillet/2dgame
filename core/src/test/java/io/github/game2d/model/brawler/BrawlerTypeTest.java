package io.github.game2d.model.brawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class BrawlerTypeTest {

    @ParameterizedTest
    @EnumSource(BrawlerType.class)
    void createsABrawlerOfItsOwnTypeAtTheGivenPosition(BrawlerType type) {
        Brawler brawler = type.create(7, 3.5f, 4.5f);

        assertEquals(type, brawler.getType());
        assertEquals(7, brawler.getId());
        assertEquals(3.5f, brawler.getX());
        assertEquals(4.5f, brawler.getY());
    }

    @Test
    void createsTheMatchingSubclass() {
        assertInstanceOf(Shelly.class, BrawlerType.SHELLY.create(1, 0, 0));
        assertInstanceOf(Colt.class, BrawlerType.COLT.create(2, 0, 0));
    }
}
