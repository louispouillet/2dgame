package io.github.game2d.client;

import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.ProjectileState;
import io.github.game2d.protocol.WorldSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapshotBuffer {
    private static final int MAX_SNAPSHOTS = 30;
    private static final float SNAP_DISTANCE = 5f;

    private record TimedSnapshot (WorldSnapshot snapshot, double time) {}
    private final List<TimedSnapshot> snapshots = new ArrayList<>();

    public void add (WorldSnapshot snapshot, double time) {
        TimedSnapshot timeSnapshot = new TimedSnapshot(snapshot, time);
        snapshots.add(timeSnapshot);
        if (snapshots.size() > MAX_SNAPSHOTS) {
            snapshots.removeFirst();
        }
    }

    public int size () {
        return snapshots.size();
    }

    public WorldSnapshot interpolate(double renderTime) {
        if (snapshots.isEmpty()) {
            return null;
        }
        TimedSnapshot from = null;
        TimedSnapshot to = null;
        for (TimedSnapshot current : snapshots ) {
            if (current.time <= renderTime) {
                from = current;
            } else {
                to = current;
                break;
            }
        }
        if (from == null) {
            return snapshots.getFirst().snapshot;
        }
        if (to == null) {
            return from.snapshot;
        }
        float t = (float) ((renderTime - from.time) / (to.time - from.time));

        Map<Integer, BrawlerState> earlierBrawlers = new HashMap<>();
        for (BrawlerState brawler : from.snapshot.brawlers()) {
            earlierBrawlers.put(brawler.id(), brawler);
        }
        List<BrawlerState> brawlers = new ArrayList<>();
        for (BrawlerState later : to.snapshot.brawlers()) {
            BrawlerState earlier = earlierBrawlers.get(later.id());
            brawlers.add(earlier == null ? later : blend(earlier, later, t));
        }

        Map<Integer, ProjectileState> earlierProjectiles = new HashMap<>();
        for (ProjectileState projectile : from.snapshot.projectiles()) {
            earlierProjectiles.put(projectile.id(), projectile);
        }
        List<ProjectileState> projectiles = new ArrayList<>();
        for (ProjectileState later : to.snapshot.projectiles()) {
            ProjectileState earlier = earlierProjectiles.get(later.id());
            projectiles.add(earlier == null ? later : blend(earlier, later, t));
        }

        return new WorldSnapshot(to.snapshot.tick(), brawlers, projectiles, to.snapshot.match(), to.snapshot.followedId());
    }

    private static boolean tooFar(float ax, float ay, float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        return dx * dx + dy * dy > SNAP_DISTANCE * SNAP_DISTANCE;
    }

    private static BrawlerState blend(BrawlerState a, BrawlerState b, float t) {
        if (tooFar(a.x(), a.y(), b.x(), b.y())) {
            return b;
        }
        float x = lerp(a.x(), b.x(), t);
        float y = lerp(a.y(), b.y(), t);
        float facing = lerpAngle(a.facing(), b.facing(), t);
        return new BrawlerState(b.id(), b.type(), x, y, b.radius(), facing,
            b.health(), b.maxHealth(), b.ammo(), b.maxAmmo(), b.reloadProgress(),
            b.moving(), b.attacking(), b.shotsFired(), b.lastProcessedInput());
    }

    /** Interpolates an angle (radians) along the shortest path: from 170° to -170°, it goes through 180°, not through 0°. */
    private static float lerpAngle(float a, float b, float t) {
        double difference = Math.IEEEremainder(b - a, 2 * Math.PI);
        return (float) (a + difference * t);
    }

    private static ProjectileState blend(ProjectileState a, ProjectileState b, float t) {
        if (tooFar(a.x(), a.y(), b.x(), b.y())) {
            return b;
        }
        float x = lerp(a.x(), b.x(), t);
        float y = lerp(a.y(), b.y(), t);
        return new ProjectileState(b.id(), b.ownerId(), b.ownerType(), x, y, b.dirX(), b.dirY(), b.radius());
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
