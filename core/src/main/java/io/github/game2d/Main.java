package io.github.game2d;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import io.github.game2d.arena.Arena;
import io.github.game2d.arena.ArenaLoader;
import io.github.game2d.arena.SpawnPoint;
import io.github.game2d.brawler.Colt;
import io.github.game2d.brawler.Shelly;
import io.github.game2d.input.InputReader;
import io.github.game2d.input.PlayerInput;
import io.github.game2d.render.WorldRenderer;
import io.github.game2d.world.GameWorld;

public class Main extends ApplicationAdapter {
    private Arena arena;
    private GameWorld world;
    private WorldRenderer renderer;
    private InputReader inputReader;
    private Shelly colt;

    @Override
    public void create() {
        arena = ArenaLoader.parse(Gdx.files.internal("maps/arena1.txt").readString());
        world = new GameWorld();
        renderer = new WorldRenderer();
        inputReader = new InputReader(renderer.getViewport());
        SpawnPoint spawn = arena.getSpawnPoint(1);
        colt = new Shelly(spawn.x(), spawn.y());
        world.add(colt);
        SpawnPoint target = arena.getSpawnPoint(2);
        world.add(new Colt(target.x(), target.y()));
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
    }

    @Override
    public void render() {
        PlayerInput input = inputReader.read(colt);
        world.applyInput(colt, input);
        world.update(Gdx.graphics.getDeltaTime());
        renderer.render(world, arena);
    }

    @Override
    public void dispose() {
        renderer.dispose();
    }
}
