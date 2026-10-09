package io.github.game2d.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.game2d.model.Team;
import io.github.game2d.protocol.LobbyPlayerState;
import io.github.game2d.protocol.LobbyState;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerNamesTest {
    private static final LobbyState LOBBY = new LobbyState(
        List.of(new LobbyPlayerState(1, "Louis", Team.BLUE), new LobbyPlayerState(2, "Lilian", Team.RED)),
        1, "maps/arena2.txt", List.of("maps/arena2.txt"), true, true);

    @Test
    void findsTheNameOfEachPlayer() {
        PlayerNames names = PlayerNames.from(LOBBY);

        assertEquals("Louis", names.of(1));
        assertEquals("Lilian", names.of(2));
    }

    @Test
    void unknownIdGivesAnEmptyName() {
        assertEquals("", PlayerNames.from(LOBBY).of(9));
    }

    @Test
    void noLobbyGivesNoNames() {
        assertEquals("", PlayerNames.from(null).of(1));
    }
}
