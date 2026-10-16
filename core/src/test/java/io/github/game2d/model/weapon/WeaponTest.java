package io.github.game2d.model.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.model.Projectile;
import io.github.game2d.model.arena.TestArenas;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * The mechanics of {@link Weapon} are tested with a test weapon whose values are set here:
 * rebalancing {@link Revolver} or {@link Shotgun} does not break these tests.
 */
class WeaponTest {
    private static final int MAX_AMMO = 3;
    // MAX_AMMO shots spaced by the cooldown must fit in one reload time
    private static final float COOLDOWN = 0.5f;
    private static final float RELOAD_TIME = 2f;
    private static final int BURST_SIZE = 4;
    private static final float BURST_INTERVAL = 0.1f;
    private static final float GUN_OFFSET = 0.15f;
    private static final float MUZZLE_DISTANCE = 0.4f;

    private final AtomicInteger ids = new AtomicInteger();

    private static class TestWeapon extends Weapon {
        TestWeapon(AttackPattern pattern, float muzzleDistance) {
            super(MAX_AMMO, COOLDOWN, RELOAD_TIME, pattern, muzzleDistance);
        }

        TestWeapon(AttackPattern pattern) {
            this(pattern, 0);
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

    // --- How an attack unfolds ---

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
    void shotsLeaveFromTheMuzzleAheadOfTheShooter() {
        TestWeapon weapon = new TestWeapon(new SpreadPattern(1, 0), MUZZLE_DISTANCE);

        Projectile towardsX = weapon.fire(1, ids::incrementAndGet, 2, 3, 1, 0).get(0);
        weapon.update(COOLDOWN);
        Projectile towardsY = weapon.fire(1, ids::incrementAndGet, 2, 3, 0, 1).get(0);

        assertEquals(2 + MUZZLE_DISTANCE, towardsX.getX(), 1e-5);
        assertEquals(3, towardsX.getY(), 1e-5);
        assertEquals(2, towardsY.getX(), 1e-5);
        assertEquals(3 + MUZZLE_DISTANCE, towardsY.getY(), 1e-5);
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
    void isAttackingUntilTheLastShotOfTheBurstLeaves() {
        TestWeapon weapon = new TestWeapon();
        assertFalse(weapon.isAttacking());

        fire(weapon);
        assertTrue(weapon.isAttacking());

        fireWholeAttackRemainder(weapon);
        assertFalse(weapon.isAttacking());
    }

    @Test
    void aSpreadIsOverAsSoonAsItIsFired() {
        TestWeapon weapon = new TestWeapon(new SpreadPattern(3, 10));

        fire(weapon);

        assertFalse(weapon.isAttacking());
    }

    private void fireWholeAttackRemainder(Weapon weapon) {
        for (int i = 0; i < 500; i++) {
            weapon.update(0.01f);
            release(weapon);
        }
    }

    @Test
    void releasingWithoutAnAttackFiresNothing() {
        assertTrue(release(new TestWeapon()).isEmpty());
    }

    // --- Real weapons: their attack shape, without depending on their values ---

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
