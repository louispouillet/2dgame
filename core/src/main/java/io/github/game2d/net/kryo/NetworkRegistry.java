package io.github.game2d.net.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.github.game2d.model.Team;
import io.github.game2d.model.brawler.BrawlerType;
import io.github.game2d.model.match.MatchPhase;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.ChooseBrawler;
import io.github.game2d.protocol.ChooseMap;
import io.github.game2d.protocol.ChooseTeam;
import io.github.game2d.protocol.JoinAccepted;
import io.github.game2d.protocol.JoinRejected;
import io.github.game2d.protocol.JoinRequest;
import io.github.game2d.protocol.LobbyPlayerState;
import io.github.game2d.protocol.LobbyState;
import io.github.game2d.protocol.MatchState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.ProjectileState;
import io.github.game2d.protocol.RefusalReason;
import io.github.game2d.protocol.StartRequest;
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
        kryo.register(Team.class);
        kryo.register(RefusalReason.class);
        kryo.register(JoinRequest.class);
        kryo.register(JoinRejected.class);
        kryo.register(ChooseTeam.class);
        kryo.register(ChooseMap.class);
        kryo.register(StartRequest.class);
        kryo.register(LobbyPlayerState.class);
        kryo.register(LobbyState.class);
        kryo.register(MatchPhase.class);
        kryo.register(MatchState.class);
        kryo.register(ChooseBrawler.class);
    }
}
