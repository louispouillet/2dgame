package io.github.game2d.net.kryo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
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
import java.util.List;
import org.junit.jupiter.api.Test;

class NetworkRegistryTest {
    // Plays the network: one Kryo writes (the server), another reads back (the client)
    private static Object roundTrip(Object message) {
        Kryo sender = new Kryo();
        Kryo receiver = new Kryo();
        NetworkRegistry.register(sender);
        NetworkRegistry.register(receiver);

        Output output = new Output(1024, -1);
        sender.writeClassAndObject(output, message);

        return receiver.readClassAndObject(new Input(output.toBytes()));
    }

    @Test
    void playerInputSurvivesTheNetwork() {
        PlayerInput input = new PlayerInput(7, 1, 0, 0.6f, 0.8f, true);

        assertEquals(input, roundTrip(input));
    }

    @Test
    void joinAcceptedSurvivesTheNetwork() {
        JoinAccepted accepted = new JoinAccepted(3);

        assertEquals(accepted, roundTrip(accepted));
    }

    @Test
    void worldSnapshotSurvivesTheNetwork() {
        WorldSnapshot snapshot = new WorldSnapshot(
            42,
            new ArrayList<>(List.of(
                new BrawlerState(1, BrawlerType.SHELLY, 1.5f, 2.5f, 0.25f, 1.25f, 120, 120, 3, 3, 0f, true, false, 4, 7),
                new BrawlerState(2, BrawlerType.COLT, 4.5f, 2.5f, 0.25f, -2.5f, 80, 100, 1, 3, 0.4f, false, true, 9, 5))),
            new ArrayList<>(List.of(
                new ProjectileState(3, 2f, 2.5f, 0.05f, 0, 1f, 0f, null))), null);

        assertEquals(snapshot, roundTrip(snapshot));
    }

    @Test
    void emptyWorldSnapshotSurvivesTheNetwork() {
        WorldSnapshot snapshot = new WorldSnapshot(0, new ArrayList<>(), new ArrayList<>(), null);

        assertEquals(snapshot, roundTrip(snapshot));
    }

    @Test
    void lobbyRequestsSurviveTheNetwork() {
        assertEquals(new JoinRequest("Louis"), roundTrip(new JoinRequest("Louis")));
        assertEquals(new ChooseTeam(Team.RED), roundTrip(new ChooseTeam(Team.RED)));
        assertEquals(new ChooseBrawler(BrawlerType.SHELLY), roundTrip(new ChooseBrawler(BrawlerType.SHELLY)));
        assertEquals(new ChooseMap("maps/arena1.txt"), roundTrip(new ChooseMap("maps/arena1.txt")));
        assertEquals(new StartRequest(), roundTrip(new StartRequest()));
    }

    @Test
    void joinRejectedSurvivesTheNetwork() {
        JoinRejected rejected = new JoinRejected(RefusalReason.NAME_TAKEN);

        assertEquals(rejected, roundTrip(rejected));
    }

    @Test
    void lobbyStateSurvivesTheNetwork() {
        LobbyState lobby = new LobbyState(
            new ArrayList<>(List.of(
                new LobbyPlayerState(1, "Louis", Team.BLUE, BrawlerType.COLT),
                new LobbyPlayerState(2, "Élodie", Team.RED, BrawlerType.SHELLY))),
            1,
            "maps/arena2.txt",
            new ArrayList<>(List.of("maps/arena1.txt", "maps/arena2.txt")),
            true,
            false);

        assertEquals(lobby, roundTrip(lobby));
    }

    @Test
    void unregisteredClassIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> roundTrip(new Object()));
    }

    @Test
    void matchStateSurvivesTheNetwork() {
        WorldSnapshot snapshot = new WorldSnapshot(7, new ArrayList<>(), new ArrayList<>(),
            new MatchState(MatchPhase.ROUND_OVER, 2, 1, 1, 2.5f, Team.RED, null));

        assertEquals(snapshot, roundTrip(snapshot));
    }

    @Test
    void projectileStateSurvivesTheNetwork() {
        WorldSnapshot snapshot = new WorldSnapshot(3, new ArrayList<>(),
            new ArrayList<>(List.of(new ProjectileState(9, 2f, 3f, 0.1f, 4, 0.6f, 0.8f, BrawlerType.COLT))), null);

        assertEquals(snapshot, roundTrip(snapshot));
    }
}
