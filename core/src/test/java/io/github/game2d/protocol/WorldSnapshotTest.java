package io.github.game2d.protocol;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.game2d.model.brawler.BrawlerType;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorldSnapshotTest {
    private static BrawlerState brawler(int id) {
        return new BrawlerState(id, BrawlerType.COLT, 1f, 1f, 0.25f, 0f, 100, 100, 3, 3, 0f, false, false, 0, 0);
    }

    @Test
    void findsABrawlerById() {
        BrawlerState second = brawler(2);
        WorldSnapshot snapshot = new WorldSnapshot(1, List.of(brawler(1), second), List.of(), null, WorldSnapshot.NOBODY);

        assertSame(second, snapshot.findBrawler(2));
    }

    @Test
    void returnsNullForABrawlerNotInTheSnapshot() {
        WorldSnapshot snapshot = new WorldSnapshot(1, List.of(brawler(1)), List.of(), null, WorldSnapshot.NOBODY);

        assertNull(snapshot.findBrawler(3));
    }
}
