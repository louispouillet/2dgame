package io.github.game2d.server;

import io.github.game2d.model.arena.ArenaLoader;
import io.github.game2d.net.kryo.NetworkRegistry;
import io.github.game2d.net.kryo.NetworkServer;
import io.github.game2d.server.GameServer;
import java.io.IOException;

public class ServerLauncher {
    public static void main(String[] args) throws IOException, InterruptedException {
        GameServer gameServer = new GameServer(ArenaLoader.availableMaps(), ArenaLoader.DEFAULT_MAP, ArenaLoader::loadResource);
        NetworkServer networkServer = new NetworkServer(gameServer);
        networkServer.start(NetworkRegistry.DEFAULT_TCP_PORT, NetworkRegistry.DEFAULT_UDP_PORT);
        gameServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            networkServer.stop();
            gameServer.stop();
            System.out.println("Server stopped.");
        }));

        System.out.println("Server listening on TCP " + NetworkRegistry.DEFAULT_TCP_PORT
            + " / UDP " + NetworkRegistry.DEFAULT_UDP_PORT + " (Ctrl+C to stop)");
        // The server threads are daemons: block here to keep the program alive
        Thread.currentThread().join();
    }
}
