package io.github.game2d.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    // Distance parcourue par Shelly en un tick : mesurée, pour ne pas dépendre de sa vitesse
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

        // Le serveur a traité les 3 inputs mais n'a pas fait avancer le brawler (obstacle inconnu du client).
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

    private static BrawlerState serverState(float x, float y, int lastProcessedInput) {
        return new BrawlerState(1, x, y, 0.25f, 120, 120, 3, 3, 1f, BrawlerType.SHELLY, lastProcessedInput);
    }
}
