package io.github.game2d.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.TestArenas;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.model.brawler.Shelly;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.PlayerInput;
import org.junit.jupiter.api.Test;

class PredictedPlayerTest {
    private static final float DT = 1f / 30;
    private static final float DELTA = 1e-4f;
    private static final float START_X = 5f;
    private static final float START_Y = 5f;
    private final Arena arena = TestArenas.open(20, 12);
    // Distance Shelly covers in one tick: measured, so it does not depend on her speed
    private final float step = measureStep();

    private float measureStep() {
        Shelly shelly = new Shelly(1, START_X, START_Y);
        shelly.setDirection(1, 0);
        shelly.update(DT, arena);
        return shelly.getX() - START_X;
    }

    private static PlayerInput move(int sequence, float moveX, float moveY) {
        return new PlayerInput(sequence, moveX, moveY, 0, 0, false);
    }

    @Test
    void movesImmediatelyWithoutWaitingForTheServer() {
        PredictedPlayer player = new PredictedPlayer(arena, DT, new Shelly(1, START_X, START_Y));

        player.apply(move(1, 1, 0));

        assertEquals(START_X + step, player.getX(), DELTA);
        assertEquals(START_Y, player.getY(), DELTA);
    }

    @Test
    void keepsItsPositionWhenTheServerAgrees() {
        PredictedPlayer player = new PredictedPlayer(arena, DT, new Shelly(1, START_X, START_Y));
        player.apply(move(1, 1, 0));
        float predictedX = player.getX();

        player.reconcile(serverState(predictedX, START_Y, 1));

        assertEquals(predictedX, player.getX(), DELTA);
        assertEquals(START_Y, player.getY(), DELTA);
    }

    @Test
    void replaysTheInputsTheServerHasNotProcessedYet() {
        PredictedPlayer player = new PredictedPlayer(arena, DT, new Shelly(1, START_X, START_Y));
        player.apply(move(1, 1, 0));
        player.apply(move(2, 1, 0));
        player.apply(move(3, 1, 0));
        float serverX = START_X + step;

        player.reconcile(serverState(serverX, START_Y, 1));

        assertEquals(START_X + 3 * step, player.getX(), DELTA);
    }

    @Test
    void followsTheServerWhenItCorrectsThePrediction() {
        PredictedPlayer player = new PredictedPlayer(arena, DT, new Shelly(1, START_X, START_Y));
        player.apply(move(1, 1, 0));
        player.apply(move(2, 1, 0));
        player.apply(move(3, 1, 0));

        // The server processed the 3 inputs but did not move the brawler (an obstacle the client does not know).
        player.reconcile(serverState(START_X, START_Y, 3));

        assertEquals(START_X, player.getX(), DELTA);
        assertEquals(START_Y, player.getY(), DELTA);
    }

    @Test
    void ignoresASnapshotOlderThanOneAlreadyApplied() {
        PredictedPlayer player = new PredictedPlayer(arena, DT, new Shelly(1, START_X, START_Y));
        player.apply(move(1, 1, 0));
        player.apply(move(2, 1, 0));
        player.apply(move(3, 1, 0));
        player.reconcile(serverState(START_X + 2 * step, START_Y, 2));

        player.reconcile(serverState(START_X + step, START_Y, 1));

        assertEquals(START_X + 3 * step, player.getX(), DELTA);
    }

    @Test
    void facesItsMovementWithoutWaitingForTheServer() {
        PredictedPlayer player = new PredictedPlayer(arena, DT, new Shelly(1, START_X, START_Y));

        player.apply(move(1, 0, 1));

        assertTrue(player.isMoving());
        assertEquals((float) (Math.PI / 2), player.getFacing(), DELTA);
    }

    @Test
    void stopsMovingWithoutWaitingForTheServer() {
        PredictedPlayer player = new PredictedPlayer(arena, DT, new Shelly(1, START_X, START_Y));
        player.apply(move(1, 1, 0));

        player.apply(move(2, 0, 0));

        assertFalse(player.isMoving());
    }

    @Test
    void takesTheFacingOfTheServerWhenStandingStill() {
        PredictedPlayer player = new PredictedPlayer(arena, DT, new Shelly(1, START_X, START_Y));
        player.apply(move(1, 0, 0));

        player.reconcile(serverState(START_X, START_Y, 1, (float) Math.PI));

        assertEquals((float) Math.PI, player.getFacing(), DELTA);
    }

    @Test
    void replaysTheMovementOverTheFacingOfTheServer() {
        PredictedPlayer player = new PredictedPlayer(arena, DT, new Shelly(1, START_X, START_Y));
        player.apply(move(1, 0, 0));
        player.apply(move(2, 1, 0));

        player.reconcile(serverState(START_X, START_Y, 1, (float) Math.PI));

        assertEquals(0f, player.getFacing(), DELTA);
    }

    private static BrawlerState serverState(float x, float y, int lastProcessedInput) {
        return serverState(x, y, lastProcessedInput, 0f);
    }

    private static BrawlerState serverState(float x, float y, int lastProcessedInput, float facing) {
        return new BrawlerState(1, BrawlerType.SHELLY, x, y, 0.25f, facing, 120, 120, 3, 3, 1f, false, false, 0, lastProcessedInput);
    }
}
