package io.github.game2d.protocol;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.game2d.model.brawler.BrawlerType;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorldSnapshotTest {
    private static BrawlerState brawler(int id) {
        return new BrawlerState(id, 1f, 1f, 0.25f, 100, 100, 3, 3, 0f, BrawlerType.COLT, 0);
    }

    @Test
    void findsABrawlerById() {
        BrawlerState second = brawler(2);
        WorldSnapshot snapshot = new WorldSnapshot(1, List.of(brawler(1), second), List.of());

        assertSame(second, snapshot.findBrawler(2));
    }

    @Test
    void returnsNullForABrawlerNotInTheSnapshot() {
        WorldSnapshot snapshot = new WorldSnapshot(1, List.of(brawler(1)), List.of());

        assertNull(snapshot.findBrawler(3));
    }
}
