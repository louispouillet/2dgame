package io.github.game2d.client;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.GdxRuntimeException;
import io.github.game2d.client.InputReader;
import io.github.game2d.client.WorldRenderer;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.ArenaLoader;
import io.github.game2d.net.LocalChannel;
import io.github.game2d.net.ServerChannel;
import io.github.game2d.net.kryo.NetworkClient;
import io.github.game2d.net.kryo.NetworkRegistry;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.WorldSnapshot;
import io.github.game2d.server.GameServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

public class Main extends ApplicationAdapter {
    private static final float DELAY = 0.1f;
    private Arena arena;
    private WorldRenderer renderer;
    private WorldRenderer3D renderer3d;
    private GameView view;
    private InputReader inputReader;
    private GameServer server;
    private ServerChannel connection;
    private NetworkClient remote;
    private SnapshotBuffer buffer;
    private final ServerTimeEstimator estimator = new ServerTimeEstimator(GameServer.TICK_RATE);
    private WorldSnapshot lastReceived;
    private IntSupplier brawlerId;
    private final String host; // null = jeu local

    public Main() {
        this.host = null;
    }

    public Main(String host) {
        this.host = host;
    }

    @Override
    public void create() {
        arena = ArenaLoader.parse(Gdx.files.internal("maps/arena2.txt").readString());
        renderer = new WorldRenderer(arena.getWidth(), arena.getHeight());
        renderer3d = new WorldRenderer3D(arena);
        view = renderer;
        inputReader = new InputReader(() -> view);
        buffer = new SnapshotBuffer();
        if (host == null) {
            startLocalGame();
        } else {
            connectTo(host);
        }
    }

    private void startLocalGame() {
        server = new GameServer(arena);
        LocalChannel channel = new LocalChannel();
        int id = server.join(channel);
        brawlerId = () -> id;
        connection = channel;
        server.join(new LocalChannel());
        server.start();
    }

    private void connectTo(String host) {
        remote = new NetworkClient();
        connection = remote.channel();
        brawlerId = remote.channel()::getBrawlerId;
        try {
            remote.connect(host, NetworkRegistry.DEFAULT_TCP_PORT, NetworkRegistry.DEFAULT_UDP_PORT);
        } catch (IOException e) {
            throw new GdxRuntimeException("Cannot connect to " + host, e);
        }
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
        renderer3d.resize(width, height);
    }

    @Override
    public void render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.V)) {
            view = view == renderer ? renderer3d : renderer;
        }
        double now = System.nanoTime() / 1_000_000_000.0;
        int myId = brawlerId.getAsInt();
        WorldSnapshot snapshot = connection.latestSnapshot();
        if (snapshot == null) {
            return;
        }
        if (snapshot != lastReceived) {
            estimator.observe(snapshot.tick(), now);
            buffer.add(snapshot, estimator.timeOfTick(snapshot.tick()));
            lastReceived = snapshot;
        }
        BrawlerState mine = null;
        for (BrawlerState brawler : snapshot.brawlers()) {
            if (brawler.id() == myId) {
                mine = brawler;
                PlayerInput input = inputReader.read(brawler.x(), brawler.y());
                connection.sendInput(input);
            }
        }
        WorldSnapshot interpolated = buffer.interpolate(estimator.estimateAt(now) - DELAY);
        view.render(withOwnBrawler(interpolated, mine), arena, myId);
    }

    // Other players are interpolated (shown slightly in the past); our own brawler is drawn as last received.
    private static WorldSnapshot withOwnBrawler(WorldSnapshot view, BrawlerState mine) {
        if (mine == null) {
            return view;
        }
        List<BrawlerState> brawlers = new ArrayList<>();
        for (BrawlerState brawler : view.brawlers()) {
            brawlers.add(brawler.id() == mine.id() ? mine : brawler);
        }
        return new WorldSnapshot(view.tick(), brawlers, view.projectiles());
    }

    @Override
    public void dispose() {
        if (server != null) server.stop();
        if (remote != null) remote.stop();
        renderer.dispose();
        renderer3d.dispose();
    }
}
