package io.github.game2d.net.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.JoinAccepted;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.ProjectileState;
import io.github.game2d.protocol.WorldSnapshot;
import java.util.ArrayList;

public class NetworkRegistry {
    public static final int DEFAULT_TCP_PORT = 54555;
    public static final int DEFAULT_UDP_PORT = 54777;

    public static void register(Kryo kryo) {
        kryo.register(PlayerInput.class);
        kryo.register(WorldSnapshot.class);
        kryo.register(BrawlerState.class);
        kryo.register(ProjectileState.class);
        kryo.register(JoinAccepted.class);
        kryo.register(ArrayList.class);
        kryo.register(BrawlerType.class);
    }
}
