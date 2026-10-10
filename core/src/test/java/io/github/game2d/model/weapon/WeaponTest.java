package io.github.game2d.model.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.model.Projectile;
import io.github.game2d.model.arena.TestArenas;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * La mécanique de {@link Weapon} est testée avec une arme de test aux valeurs fixées ici :
 * changer l'équilibrage de {@link Revolver} ou {@link Shotgun} ne casse pas ces tests.
 */
class WeaponTest {
    private static final int MAX_AMMO = 3;
    // MAX_AMMO tirs espacés du cooldown doivent tenir dans un temps de recharge
    private static final float COOLDOWN = 0.5f;
    private static final float RELOAD_TIME = 2f;
    private static final int BURST_SIZE = 4;
    private static final float BURST_INTERVAL = 0.1f;
    private static final float GUN_OFFSET = 0.15f;

    private final AtomicInteger ids = new AtomicInteger();

    private static class TestWeapon extends Weapon {
        TestWeapon(AttackPattern pattern) {
            super(MAX_AMMO, COOLDOWN, RELOAD_TIME, pattern);
        }

        TestWeapon() {
            this(new BurstPattern(BURST_SIZE, BURST_INTERVAL, GUN_OFFSET));
        }

        @Override
        protected Projectile createProjectile(int id, int ownerId, float x, float y, float dirX, float dirY) {
            return new Projectile(id, ownerId, x, y, 0.1f, dirX, dirY, 10f, 5f, 10);
        }
    }

    private List<Projectile> fire(Weapon weapon) {
        return weapon.fire(1, ids::incrementAndGet, 0, 0, 1, 0);
    }

    private List<Projectile> release(Weapon weapon) {
        return weapon.releaseShots(1, ids::incrementAndGet, 0, 0);
    }

    /** Fires one attack at (0, 0) towards +x and collects every projectile released until the attack is over. */
    private List<Projectile> fireWholeAttack(Weapon weapon) {
        List<Projectile> shots = new ArrayList<>(fire(weapon));
        for (int i = 0; i < 500; i++) {
            weapon.update(0.01f);
            shots.addAll(release(weapon));
        }
        return shots;
    }

    // --- Munitions, cooldown, recharge ---

    @Test
    void firingConsumesOneAmmo() {
        TestWeapon weapon = new TestWeapon();

        fire(weapon);

        assertEquals(MAX_AMMO - 1, weapon.getAmmo());
    }

    @Test
    void cannotFireDuringCooldown() {
        TestWeapon weapon = new TestWeapon();
        fire(weapon);

        weapon.update(COOLDOWN * 0.9f);

        assertTrue(fire(weapon).isEmpty());
        assertEquals(MAX_AMMO - 1, weapon.getAmmo());
    }

    @Test
    void canFireAgainOnceCooldownIsOver() {
        TestWeapon weapon = new TestWeapon();
        fire(weapon);

        weapon.update(COOLDOWN + 0.01f);

        assertEquals(1, fire(weapon).size());
    }

    @Test
    void cannotFireWithoutAmmo() {
        TestWeapon weapon = new TestWeapon();
        for (int i = 0; i < MAX_AMMO; i++) {
            fire(weapon);
            weapon.update(COOLDOWN + 0.01f);
        }
        assertEquals(0, weapon.getAmmo());

        // the cooldown is over but no ammo has come back yet
        assertTrue(fire(weapon).isEmpty());
    }

    @Test
    void reloadsOneAmmoAfterReloadTime() {
        TestWeapon weapon = new TestWeapon();
        fire(weapon);

        weapon.update(RELOAD_TIME);

        assertEquals(MAX_AMMO, weapon.getAmmo());
    }

    @Test
    void reloadProgressGrowsWhileReloading() {
        TestWeapon weapon = new TestWeapon();
        fire(weapon);

        weapon.update(RELOAD_TIME / 2);

        assertEquals(0.5f, weapon.getReloadProgress(), 1e-4);
    }

    @Test
    void reloadProgressStaysAtZeroWhenFull() {
        TestWeapon weapon = new TestWeapon();

        weapon.update(5 * RELOAD_TIME);

        assertEquals(0f, weapon.getReloadProgress(), 1e-6);
        assertEquals(MAX_AMMO, weapon.getAmmo());
    }

    // --- Déroulement d'une attaque ---

    @Test
    void onlyTheFirstShotOfABurstLeavesImmediately() {
        List<Projectile> shots = fire(new TestWeapon());

        assertEquals(1, shots.size());
        assertEquals(1, shots.get(0).getOwnerId());
    }

    @Test
    void oneFireReleasesTheWholeBurst() {
        List<Projectile> shots = fireWholeAttack(new TestWeapon());

        assertEquals(BURST_SIZE, shots.size());
        assertEquals(BURST_SIZE, shots.stream().map(Projectile::getId).distinct().count());
    }

    @Test
    void burstShotsWaitForTheirDelay() {
        TestWeapon weapon = new TestWeapon();
        fire(weapon);

        weapon.update(BURST_INTERVAL / 2);
        assertTrue(release(weapon).isEmpty());

        weapon.update(BURST_INTERVAL / 2 + 0.01f);
        assertEquals(1, release(weapon).size());
    }

    @Test
    void sideOffsetIsAppliedToTheRightOfTheAim() {
        List<Projectile> shots = fireWholeAttack(new TestWeapon());

        // aiming towards +x, the right-hand side is -y
        assertEquals(-GUN_OFFSET, shots.get(0).getY(), 1e-5);
        assertEquals(GUN_OFFSET, shots.get(1).getY(), 1e-5);
    }

    @Test
    void delayedShotsStartFromTheShootersCurrentPosition() {
        TestWeapon weapon = new TestWeapon();
        fire(weapon);
        weapon.update(BURST_INTERVAL + 0.01f);

        List<Projectile> shots = weapon.releaseShots(1, ids::incrementAndGet, 5, 3);

        assertEquals(5, shots.get(0).getX(), 1e-5);
        assertEquals(3 + GUN_OFFSET, shots.get(0).getY(), 1e-5);
    }

    @Test
    void angleRotatesTheShotAroundTheAim() {
        TestWeapon weapon = new TestWeapon(new SpreadPattern(3, 90));

        List<Projectile> shots = fire(weapon);
        shots.forEach(p -> p.update(0.1f, TestArenas.open(30, 20)));

        // -90°, 0°, +90° around +x: towards -y, +x, +y
        assertTrue(shots.get(0).getY() < 0);
        assertEquals(0, shots.get(1).getY(), 1e-5);
        assertTrue(shots.get(1).getX() > 0);
        assertTrue(shots.get(2).getY() > 0);
    }

    @Test
    void releasingWithoutAnAttackFiresNothing() {
        assertTrue(release(new TestWeapon()).isEmpty());
    }

    // --- Armes réelles : leur forme d'attaque, sans dépendre de leurs valeurs ---

    @Test
    void revolverFiresAnAlternatingBurst() {
        List<Projectile> shots = fireWholeAttack(new Revolver());

        assertTrue(shots.size() > 1);
        for (int i = 1; i < shots.size(); i++) {
            assertEquals(-Math.signum(shots.get(i - 1).getY()), Math.signum(shots.get(i).getY()), "bullet " + i);
        }
    }

    @Test
    void shotgunFiresSeveralPelletsAtOnce() {
        List<Projectile> shots = fire(new Shotgun());

        assertTrue(shots.size() > 1);
        assertEquals(shots.size(), shots.stream().map(Projectile::getId).distinct().count());
    }

    @Test
    void shotgunPelletsSpreadSymmetricallyAroundTheAim() {
        List<Projectile> shots = fire(new Shotgun());
        shots.forEach(p -> p.update(0.1f, TestArenas.open(30, 20)));

        int n = shots.size();
        for (int i = 0; i < n / 2; i++) {
            assertEquals(shots.get(i).getY(), -shots.get(n - 1 - i).getY(), 1e-5);
        }
        assertTrue(shots.get(n - 1).getY() > shots.get(0).getY());
    }
}
