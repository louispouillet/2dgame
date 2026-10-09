package io.github.game2d.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.game2d.model.Team;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.TestArenas;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.net.LocalChannel;
import io.github.game2d.net.ServerChannel;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.RefusalReason;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClientGameTest {
    private static final float TICK = 1f / 30;
    private static final float DELTA = 1e-4f;
    private static final int MY_ID = 1;
    private static final int ENEMY_ID = 2;
    private static final float START_X = 5f;
    private static final float START_Y = 5f;
    private static final float SHELLY_SPEED = 4f;

    private final Arena arena = TestArenas.open(20, 12);
    private final LocalChannel channel = new LocalChannel();
    private final ClientGame game = new ClientGame(arena, new FakeSession(channel, MY_ID));

    private record FakeSession(ServerChannel channel, int playerId) implements GameSession {
        @Override
        public void join(String name) {
        }

        @Override
        public void chooseTeam(Team team) {
        }

        @Override
        public void chooseMap(String map) {
        }

        @Override
        public void start() {
        }

        @Override
        public RefusalReason rejection() {
            return null;
        }

        @Override
        public void close() {
        }
    }

    private static BrawlerState brawler(int id, float x, float y) {
        return new BrawlerState(id, x, y, 0.25f, 120, 120, 3, 3, 0f, BrawlerType.SHELLY, 0);
    }

    /** Remplace le clavier (Gdx.input n'existe pas hors d'une fenêtre libGDX). */
    private static InputReader moveRight() {
        return new InputReader(null) {
            @Override
            public PlayerInput read(float playerX, float playerY) {
                return new PlayerInput(1, 1, 0, 0, 0, false);
            }
        };
    }

    private WorldSnapshot playOneTick() {
        game.update(0, moveRight());
        return game.update(1.5 * TICK, moveRight());
    }

    private static BrawlerState find(WorldSnapshot snapshot, int id) {
        return snapshot.brawlers().stream().filter(b -> b.id() == id).findFirst().orElseThrow();
    }

    @Test
    void hasNothingToDrawBeforeTheFirstSnapshot() {
        assertNull(game.update(0, moveRight()));
    }

    @Test
    void sendsTheInputReadAtEachTick() {
        channel.sendSnapshot(new WorldSnapshot(1, List.of(brawler(MY_ID, START_X, START_Y)), List.of()));

        playOneTick();

        assertEquals(1, channel.latestInput().moveX());
    }

    @Test
    void sendsNoInputWhileItsBrawlerIsNotInTheSnapshot() {
        channel.sendSnapshot(new WorldSnapshot(1, List.of(brawler(ENEMY_ID, START_X, START_Y)), List.of()));

        playOneTick();

        assertEquals(0, channel.latestInput().sequence());
    }

    @Test
    void drawsItsOwnBrawlerAtThePredictedPosition() {
        channel.sendSnapshot(new WorldSnapshot(1, List.of(brawler(MY_ID, START_X, START_Y)), List.of()));

        WorldSnapshot drawn = playOneTick();

        assertEquals(START_X + SHELLY_SPEED * TICK, find(drawn, MY_ID).x(), DELTA);
    }

    @Test
    void drawsOtherBrawlersAsReceived() {
        channel.sendSnapshot(new WorldSnapshot(1,
            List.of(brawler(MY_ID, START_X, START_Y), brawler(ENEMY_ID, 8f, 6f)), List.of()));

        WorldSnapshot drawn = playOneTick();

        assertEquals(8f, find(drawn, ENEMY_ID).x(), DELTA);
        assertEquals(6f, find(drawn, ENEMY_ID).y(), DELTA);
    }
}
