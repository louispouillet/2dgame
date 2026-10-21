package io.github.game2d.client;

/** Tells how strong the red flash on my screen is: it starts when my health drops, then fades out quickly. */
public final class HitFlash {
    /** Length of the flash, in seconds. */
    public static final float DURATION = 0.1f;
    private static final int UNKNOWN = -1;

    private int lastHealth = UNKNOWN;
    private float timeLeft;

    /** Regeneration and a new round raise the health: only a drop is a hit. */
    public void update(int health, float frameTime) {
        if (lastHealth != UNKNOWN && health < lastHealth) {
            timeLeft = DURATION;
        } else {
            timeLeft = Math.max(0f, timeLeft - frameTime);
        }
        lastHealth = health;
    }

    /** My brawler is not in the snapshot (dead): no flash, and its next appearance starts afresh. */
    public void reset() {
        lastHealth = UNKNOWN;
        timeLeft = 0f;
    }

    /** 1 right after the hit, down to 0. Squared, so the red drops fast then ends smoothly. */
    public float intensity() {
        float left = timeLeft / DURATION;
        return left * left;
    }
}
