package io.github.game2d.client;

import io.github.game2d.protocol.LobbyPlayerState;
import io.github.game2d.protocol.LobbyState;
import java.util.HashMap;
import java.util.Map;

public final class PlayerNames {
    private final Map<Integer, String> names;

    private PlayerNames(Map<Integer, String> names) {
        this.names = Map.copyOf(names);
    }

    public static PlayerNames from(LobbyState lobby) {
        Map<Integer, String> names = new HashMap<>();
        if (lobby != null) {
            for (LobbyPlayerState player : lobby.players()) {
                names.put(player.id(), player.name());
            }
        }
        return new PlayerNames(names);
    }

    public String of(int brawlerId) {
        return names.getOrDefault(brawlerId, "");
    }
}
