package io.github.game2d.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.model.match.MatchPhase;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.MatchState;
import io.github.game2d.protocol.ProjectileState;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class SnapshotBufferTest {
    private static final float DELTA = 1e-4f;

    private static BrawlerState brawler(int id, float x, float y, int health) {
        return new BrawlerState(id, BrawlerType.SHELLY, x, y, 0.4f, 0f, health, 100, 3, 3, 0f, false, false, 0, 0);
    }

    private static BrawlerState animated(float facing, boolean moving, int shotsFired) {
        return new BrawlerState(1, BrawlerType.SHELLY, 10, 0, 0.4f, facing, 100, 100, 3, 3, 0f, moving, false, shotsFired, 0);
    }

    @Test
    void facingIsInterpolatedBetweenTwoSnapshots() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        buffer.add(world(animated(0f, false, 0)), 1.0);
        buffer.add(world(animated(1f, false, 0)), 1.1);

        assertEquals(0.5f, buffer.interpolate(1.05).brawlers().get(0).facing(), DELTA);
    }

    @Test
    void facingTurnsTheShortestWayRound() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        // just under +180° then just over -180°: a small turn, not a full half turn through 0
        buffer.add(world(animated(3.0f, false, 0)), 1.0);
        buffer.add(world(animated(-3.0f, false, 0)), 1.1);

        float facing = buffer.interpolate(1.05).brawlers().get(0).facing();

        assertEquals(Math.PI, Math.abs(facing), 1e-3);
    }

    @Test
    void movementAndShotsComeFromTheLatestSnapshot() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        buffer.add(world(animated(0f, false, 4)), 1.0);
        buffer.add(world(animated(0f, true, 5)), 1.1);

        BrawlerState result = buffer.interpolate(1.05).brawlers().get(0);

        assertTrue(result.moving());
        assertEquals(5, result.shotsFired());
    }

    private static WorldSnapshot world(BrawlerState... brawlers) {
        return new WorldSnapshot(0, List.of(brawlers), List.of(), null, WorldSnapshot.NOBODY);
    }

    @Test
    void emptyBufferHasNothingToShow() {
        assertNull(new SnapshotBuffer().interpolate(1.0));
    }

    @Test
    void singleSnapshotIsReturnedAsIs() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        WorldSnapshot only = world(brawler(1, 10, 0, 100));
        buffer.add(only, 1.0);

        assertEquals(only, buffer.interpolate(5.0));
    }

    @Test
    void positionIsInterpolatedBetweenTwoSnapshots() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        buffer.add(world(brawler(1, 10, 0, 100)), 1.00);
        buffer.add(world(brawler(1, 13, 2, 100)), 1.10);

        BrawlerState result = buffer.interpolate(1.06).brawlers().get(0);

        assertEquals(11.8f, result.x(), DELTA);
        assertEquals(1.2f, result.y(), DELTA);
    }

    @Test
    void renderTimeBeforeTheFirstSnapshotClampsToIt() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        buffer.add(world(brawler(1, 10, 0, 100)), 1.0);
        buffer.add(world(brawler(1, 13, 0, 100)), 1.1);

        assertEquals(10f, buffer.interpolate(0.5).brawlers().get(0).x(), DELTA);
    }

    @Test
    void renderTimeAfterTheLastSnapshotHoldsIt() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        buffer.add(world(brawler(1, 10, 0, 100)), 1.0);
        buffer.add(world(brawler(1, 13, 0, 100)), 1.1);

        assertEquals(13f, buffer.interpolate(9.0).brawlers().get(0).x(), DELTA);
    }

    @Test
    void picksTheTwoSnapshotsAroundTheRenderTime() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        buffer.add(world(brawler(1, 0, 0, 100)), 1.0);
        buffer.add(world(brawler(1, 2, 0, 100)), 1.1);
        buffer.add(world(brawler(1, 6, 0, 100)), 1.2);

        assertEquals(4f, buffer.interpolate(1.15).brawlers().get(0).x(), DELTA);
    }

    @Test
    void discreteValuesComeFromTheLaterSnapshot() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        buffer.add(world(brawler(1, 0, 0, 100)), 1.0);
        buffer.add(world(brawler(1, 0, 0, 60)), 1.1);

        assertEquals(60, buffer.interpolate(1.01).brawlers().get(0).health());
    }

    @Test
    void entityMissingFromTheEarlierSnapshotAppearsAsIs() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        buffer.add(world(brawler(1, 0, 0, 100)), 1.0);
        buffer.add(world(brawler(1, 0, 0, 100), brawler(2, 5, 5, 100)), 1.1);

        List<BrawlerState> result = buffer.interpolate(1.05).brawlers();

        assertEquals(2, result.size());
        assertEquals(5f, result.get(1).x(), DELTA);
    }

    @Test
    void entityMissingFromTheLaterSnapshotIsDropped() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        buffer.add(world(brawler(1, 0, 0, 100), brawler(2, 5, 5, 100)), 1.0);
        buffer.add(world(brawler(1, 0, 0, 100)), 1.1);

        assertEquals(1, buffer.interpolate(1.05).brawlers().size());
    }

    @Test
    void projectilesAreInterpolatedToo() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        buffer.add(new WorldSnapshot(0, List.of(), List.of(new ProjectileState(7, 1, null, 0, 0, 1f, 0f, 0.1f)), null, WorldSnapshot.NOBODY), 1.0);
        buffer.add(new WorldSnapshot(0, List.of(), List.of(new ProjectileState(7, 1, null, 4, 2, 1f, 0f, 0.1f)), null, WorldSnapshot.NOBODY), 1.1);

        ProjectileState result = buffer.interpolate(1.05).projectiles().get(0);

        assertEquals(2f, result.x(), DELTA);
        assertEquals(1f, result.y(), DELTA);
    }

    @Test
    void largeJumpSnapsInsteadOfSliding() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        buffer.add(world(brawler(1, 0, 0, 100)), 1.0);
        buffer.add(world(brawler(1, 50, 0, 100)), 1.1);

        assertEquals(50f, buffer.interpolate(1.02).brawlers().get(0).x(), DELTA);
    }

    @Test
    void oldSnapshotsAreDiscarded() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        for (int i = 0; i < 100; i++) {
            buffer.add(world(brawler(1, i, 0, 100)), i * 0.1);
        }

        assertTrue(buffer.size() < 100);
    }

    @Test
    void interpolationKeepsTheMatchStateOfTheLatestSnapshot() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        MatchState countdown = new MatchState(MatchPhase.COUNTDOWN, 1, 0, 0, 2f, null, null);
        MatchState playing = new MatchState(MatchPhase.PLAYING, 1, 0, 0, 0f, null, null);
        buffer.add(new WorldSnapshot(0, List.of(), List.of(), countdown, WorldSnapshot.NOBODY), 1.0);
        buffer.add(new WorldSnapshot(1, List.of(), List.of(), playing, WorldSnapshot.NOBODY), 2.0);

        assertEquals(playing, buffer.interpolate(1.5).match());
    }

    @Test
    void interpolationFollowsTheBrawlerOfTheLatestSnapshot() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        buffer.add(new WorldSnapshot(0, List.of(), List.of(), null, 1), 1.0);
        buffer.add(new WorldSnapshot(1, List.of(), List.of(), null, 3), 2.0);

        assertEquals(3, buffer.interpolate(1.5).followedId());
    }
}
