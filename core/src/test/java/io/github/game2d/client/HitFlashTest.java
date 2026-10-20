package io.github.game2d.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HitFlashTest {
    private static final float FRAME = 1 / 60f;
    private static final float DELTA = 0.0001f;

    private final HitFlash flash = new HitFlash();

    @Test
    void noFlashWhenFirstSeen() {
        flash.update(40, FRAME);

        assertEquals(0f, flash.intensity());
    }

    @Test
    void flashesWhenHealthDrops() {
        flash.update(100, FRAME);

        flash.update(70, FRAME);

        assertEquals(1f, flash.intensity());
    }

    @Test
    void flashFadesFastThenSmoothly() {
        flash.update(100, FRAME);
        flash.update(70, FRAME);

        flash.update(70, HitFlash.DURATION / 2);
        assertEquals(0.25f, flash.intensity(), DELTA);

        flash.update(70, HitFlash.DURATION);
        assertEquals(0f, flash.intensity());
    }

    @Test
    void regenerationDoesNotFlash() {
        flash.update(70, FRAME);

        flash.update(80, FRAME);

        assertEquals(0f, flash.intensity());
    }

    @Test
    void newRoundDoesNotFlash() {
        flash.update(30, FRAME);

        flash.update(100, FRAME);

        assertEquals(0f, flash.intensity());
    }

    @Test
    void secondHitRestartsTheFlash() {
        flash.update(100, FRAME);
        flash.update(90, FRAME);
        flash.update(90, HitFlash.DURATION / 2);

        flash.update(80, FRAME);

        assertEquals(1f, flash.intensity());
    }

    @Test
    void resetStopsTheFlashAndForgetsTheHealth() {
        flash.update(100, FRAME);
        flash.update(70, FRAME);

        flash.reset();
        assertEquals(0f, flash.intensity());

        flash.update(50, FRAME);
        assertEquals(0f, flash.intensity());
    }
}
