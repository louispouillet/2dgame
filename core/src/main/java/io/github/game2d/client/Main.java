package io.github.game2d.client;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
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
import java.util.function.IntSupplier;

public class Main extends ApplicationAdapter {
    private Arena arena;
    private WorldRenderer renderer;
    private InputReader inputReader;
    private GameServer server;
    private ServerChannel connection;
    private NetworkClient remote;
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
        arena = ArenaLoader.parse(Gdx.files.internal("maps/arena1.txt").readString());
        renderer = new WorldRenderer();
        inputReader = new InputReader(renderer.getViewport());
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
    }

    @Override
    public void render() {
        int myId = brawlerId.getAsInt();
        WorldSnapshot snapshot = connection.latestSnapshot();
        if (snapshot == null) {
            return;
        }
        for (BrawlerState brawler : snapshot.brawlers()) {
            if (brawler.id() == myId) {
                PlayerInput input = inputReader.read(brawler.x(), brawler.y());
                connection.sendInput(input);
            }
        }
        renderer.render(snapshot, arena);
    }

    @Override
    public void dispose() {
        if (server != null) server.stop();
        if (remote != null) remote.stop();
        renderer.dispose();
    }
}
