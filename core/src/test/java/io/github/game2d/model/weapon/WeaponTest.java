package io.github.game2d.model.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.model.Projectile;
import io.github.game2d.model.arena.TestArenas;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WeaponTest {
    private final AtomicInteger ids = new AtomicInteger();

    private List<Projectile> fire(Weapon weapon) {
        return weapon.fire(1, ids::incrementAndGet, 0, 0, 1, 0);
    }

    @Test
    void firingConsumesOneAmmo() {
        Revolver revolver = new Revolver();

        fire(revolver);

        assertEquals(revolver.getMaxAmmo() - 1, revolver.getAmmo());
    }

    @Test
    void cannotFireDuringCooldown() {
        Revolver revolver = new Revolver();
        fire(revolver);

        assertTrue(fire(revolver).isEmpty());
        assertEquals(revolver.getMaxAmmo() - 1, revolver.getAmmo());
    }

    @Test
    void canFireAgainOnceCooldownIsOver() {
        Revolver revolver = new Revolver();
        fire(revolver);

        revolver.update(0.31f);

        assertEquals(1, fire(revolver).size());
    }

    @Test
    void cannotFireWithoutAmmo() {
        Revolver revolver = new Revolver();
        for (int i = 0; i < revolver.getMaxAmmo(); i++) {
            fire(revolver);
            revolver.update(0.31f);
        }
        assertEquals(0, revolver.getAmmo());

        // 0.31 s of reload per shot is not enough to regain a bullet (1.5 s)
        assertTrue(fire(revolver).isEmpty());
    }

    @Test
    void reloadsOneAmmoAfterReloadTime() {
        Revolver revolver = new Revolver();
        fire(revolver);

        revolver.update(1.5f);

        assertEquals(revolver.getMaxAmmo(), revolver.getAmmo());
    }

    @Test
    void reloadProgressGrowsWhileReloading() {
        Revolver revolver = new Revolver();
        fire(revolver);

        revolver.update(0.75f);

        assertEquals(0.5f, revolver.getReloadProgress(), 1e-4);
    }

    @Test
    void reloadProgressStaysAtZeroWhenFull() {
        Revolver revolver = new Revolver();

        revolver.update(5f);

        assertEquals(0f, revolver.getReloadProgress(), 1e-6);
        assertEquals(revolver.getMaxAmmo(), revolver.getAmmo());
    }

    @Test
    void revolverFiresOneProjectileFromTheOwner() {
        List<Projectile> shots = fire(new Revolver());

        assertEquals(1, shots.size());
        assertEquals(1, shots.get(0).getOwnerId());
        assertEquals(30, shots.get(0).getDamage());
    }

    @Test
    void shotgunFiresFivePelletsWithDistinctIds() {
        List<Projectile> shots = fire(new Shotgun());

        assertEquals(5, shots.size());
        assertEquals(5, shots.stream().map(Projectile::getId).distinct().count());
        assertTrue(shots.stream().allMatch(p -> p.getDamage() == 15));
    }

    @Test
    void shotgunPelletsSpreadSymmetricallyAroundTheAim() {
        List<Projectile> shots = fire(new Shotgun());
        shots.forEach(p -> p.update(0.1f, TestArenas.open(30, 20)));

        assertEquals(0, shots.get(2).getY(), 1e-5);
        assertEquals(shots.get(0).getY(), -shots.get(4).getY(), 1e-5);
        assertEquals(shots.get(1).getY(), -shots.get(3).getY(), 1e-5);
        assertTrue(shots.get(4).getY() > shots.get(3).getY());
    }
}
