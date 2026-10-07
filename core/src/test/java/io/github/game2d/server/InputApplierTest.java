package io.github.game2d.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.game2d.model.GameWorld;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.TestArenas;
import io.github.game2d.model.brawler.Colt;
import io.github.game2d.protocol.PlayerInput;
import org.junit.jupiter.api.Test;

class InputApplierTest {
    private static final Arena ARENA = TestArenas.open(30, 20);
    private static final float X0 = 10f;
    private static final float Y0 = 10f;

    @Test
    void inputIsRoutedToThePlayersBrawler() {
        GameWorld world = new GameWorld(ARENA);
        InputApplier applier = new InputApplier(world);
        Colt p1 = new Colt(world.nextId(), X0, Y0);
        Colt p2 = new Colt(world.nextId(), X0 + 5, Y0);
        world.addPlayer(p1);
        world.addPlayer(p2);

        applier.apply(p2.getId(), new PlayerInput(1, 0, 1, 0, false));
        world.update(1f);

        assertEquals(X0, p1.getX());
        assertTrue(p2.getX() > X0 + 5);
    }

    @Test
    void unknownBrawlerIsIgnored() {
        GameWorld world = new GameWorld(ARENA);
        InputApplier applier = new InputApplier(world);

        applier.apply(42, new PlayerInput(1, 0, 1, 0, true));

        assertTrue(world.getGameObjects().isEmpty());
    }
}
