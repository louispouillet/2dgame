package io.github.game2d.server;

import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.ArenaLoader;
import io.github.game2d.net.kryo.NetworkRegistry;
import io.github.game2d.net.kryo.NetworkServer;
import io.github.game2d.server.GameServer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerLauncher {
    private static final String ARENA_FILE = "assets/maps/arena1.txt";

    public static void main(String[] args) throws IOException, InterruptedException {
        Arena arena = ArenaLoader.parse(Files.readString(Path.of(ARENA_FILE)));
        GameServer gameServer = new GameServer(arena);
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
        // Les threads du serveur sont des daemons : on bloque ici pour garder le programme en vie
        Thread.currentThread().join();
    }
}
