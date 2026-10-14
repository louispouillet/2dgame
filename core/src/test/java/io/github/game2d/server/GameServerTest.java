package io.github.game2d.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.game2d.model.Team;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.ArenaLoader;
import io.github.game2d.model.arena.SpawnPoint;
import io.github.game2d.model.match.KnockoutMatch;
import io.github.game2d.model.match.MatchPhase;
import io.github.game2d.net.LocalChannel;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.LobbyState;
import io.github.game2d.protocol.MatchState;
import io.github.game2d.protocol.PlayerInput;
import io.github.game2d.protocol.RefusalReason;
import io.github.game2d.protocol.WorldSnapshot;
import io.github.game2d.time.TickClock;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameServerTest {
    private static final String MAP = """
        ######
        #1..4#
        #....#
        ######
        """;
    private static final String TEAM_MAP = """
        ########
        #1.2.4.#
        #3.5.6.#
        ########
        """;

    private GameServer server;
    private int joined;

    @BeforeEach
    void createServer() {
        Arena arena = ArenaLoader.parse(MAP);
        server = new GameServer(List.of("test"), "test", map -> arena);
    }

    private static BrawlerState brawlerOf(LocalChannel channel, int id) {
        WorldSnapshot snapshot = channel.latestSnapshot();
        return snapshot.brawlers().stream().filter(b -> b.id() == id).findFirst().orElseThrow();
    }

    private static void assertRefused(RefusalReason reason, Runnable request) {
        LobbyException error = assertThrows(LobbyException.class, request::run);
        assertEquals(reason, error.getReason());
    }

    private static void assertAt(SpawnPoint spawn, BrawlerState brawler) {
        assertEquals(spawn.x(), brawler.x());
        assertEquals(spawn.y(), brawler.y());
    }

    private int join(LocalChannel channel) {
        joined++;
        return server.join(channel, "Player" + joined);
    }

    // Adds a still opponent then starts the game
    private void startWith(int leaderId) {
        server.join(new LocalChannel(), "Opponent");
        server.requestStart(leaderId);
    }

    private void start(int leaderId) {
        server.requestStart(leaderId);
    }

    private static int ticksFor(float seconds) {
        return Math.round(seconds * TickClock.TICK_RATE) + 1;
    }

    private static void tick(GameServer target, int ticks) {
        for (int i = 0; i < ticks; i++) {
            target.tick();
        }
    }

    private void skipCountdown() {
        tick(server, ticksFor(KnockoutMatch.COUNTDOWN_TIME));
    }

    private static MatchState matchOf(LocalChannel channel) {
        return channel.latestSnapshot().match();
    }

    // The first player (Shelly, on the left) shoots to the right until the round ends
    private void shootUntilTheRoundEnds(LocalChannel shooter) {
        shooter.sendInput(new PlayerInput(1, 0, 0, 1, 0, true));
        for (int i = 0; i < ticksFor(30f) && matchOf(shooter).phase() == MatchPhase.PLAYING; i++) {
            server.tick();
        }
        shooter.sendInput(new PlayerInput(2, 0, 0, 1, 0, false));
    }

    @Test
    void joinedPlayersAreSentTheLobby() {
        LocalChannel first = new LocalChannel();
        LocalChannel second = new LocalChannel();

        int firstId = server.join(first, "Louis");
        server.join(second, "Lilian");

        LobbyState lobby = first.latestLobby();
        assertEquals(List.of("Louis", "Lilian"), lobby.players().stream().map(p -> p.name()).toList());
        assertEquals(firstId, lobby.leaderId());
        assertEquals("test", lobby.map());
        assertTrue(lobby.canStart());
        assertFalse(lobby.started());
        assertEquals(lobby, second.latestLobby());
    }

    @Test
    void refusedJoinGivesItsReason() {
        server.join(new LocalChannel(), "Louis");

        assertRefused(RefusalReason.NAME_TAKEN, () -> server.join(new LocalChannel(), "louis"));
    }

    @Test
    void refusedJoinDoesNotUseAPlayerId() {
        int first = server.join(new LocalChannel(), "Louis");
        assertThrows(LobbyException.class, () -> server.join(new LocalChannel(), "Louis"));

        int second = server.join(new LocalChannel(), "Lilian");

        assertEquals(first + 1, second);
    }

    @Test
    void noSnapshotIsSentInTheLobby() {
        LocalChannel channel = new LocalChannel();
        server.join(channel, "Louis");
        server.join(new LocalChannel(), "Lilian");

        server.tick();

        assertNull(channel.latestSnapshot());
    }

    @Test
    void teamChangeIsSentToEveryone() {
        LocalChannel channel = new LocalChannel();
        int id = server.join(channel, "Louis");

        server.chooseTeam(id, Team.RED);

        assertEquals(Team.RED, channel.latestLobby().players().get(0).team());
    }

    @Test
    void onlyTheLeaderCanStart() {
        int leader = server.join(new LocalChannel(), "Louis");
        int other = server.join(new LocalChannel(), "Lilian");

        assertRefused(RefusalReason.NOT_LEADER, () -> server.requestStart(other));

        server.requestStart(leader);
    }

    @Test
    void cannotStartWithAnEmptyTeam() {
        int leader = server.join(new LocalChannel(), "Louis");

        assertRefused(RefusalReason.TEAMS_NOT_READY, () -> server.requestStart(leader));
    }

    @Test
    void startingTellsEveryoneTheGameStarted() {
        LocalChannel channel = new LocalChannel();
        int leader = server.join(channel, "Louis");
        server.join(new LocalChannel(), "Lilian");

        start(leader);

        assertTrue(channel.latestLobby().started());
    }

    @Test
    void lobbyRequestsAreRefusedDuringAGame() {
        int leader = server.join(new LocalChannel(), "Louis");
        server.join(new LocalChannel(), "Lilian");
        start(leader);

        assertRefused(RefusalReason.GAME_IN_PROGRESS, () -> server.join(new LocalChannel(), "Tom"));
        assertRefused(RefusalReason.GAME_IN_PROGRESS, () -> server.chooseTeam(leader, Team.RED));
        assertRefused(RefusalReason.GAME_IN_PROGRESS, () -> server.chooseMap(leader, "test"));
        assertRefused(RefusalReason.GAME_IN_PROGRESS, () -> server.requestStart(leader));
    }

    @Test
    void gameIsPlayedOnTheChosenMap() {
        Map<String, Arena> arenas = Map.of(
            "small", ArenaLoader.parse(MAP),
            "large", ArenaLoader.parse(TEAM_MAP));
        GameServer choosing = new GameServer(List.of("small", "large"), "small", arenas::get);
        LocalChannel channel = new LocalChannel();
        int leader = choosing.join(channel, "Louis");
        int other = choosing.join(new LocalChannel(), "Lilian");

        choosing.chooseMap(leader, "large");
        choosing.requestStart(leader);
        choosing.tick();

        assertEquals("large", channel.latestLobby().map());
        assertEquals(5.5f, brawlerOf(channel, other).x());
    }

    @Test
    void playersAppearOnTheSpawnPointsOfTheirTeam() {
        Arena arena = ArenaLoader.parse(TEAM_MAP);
        GameServer teams = new GameServer(List.of("teams"), "teams", map -> arena);
        LocalChannel channel = new LocalChannel();
        int louis = teams.join(channel, "Louis");
        int lilian = teams.join(new LocalChannel(), "Lilian");
        int tom = teams.join(new LocalChannel(), "Tom");
        int lea = teams.join(new LocalChannel(), "Lea");
        teams.chooseTeam(louis, Team.RED);

        teams.requestStart(louis);
        teams.tick();

        assertAt(arena.getSpawnPoint(1), brawlerOf(channel, tom));
        assertAt(arena.getSpawnPoint(4), brawlerOf(channel, louis));
        assertAt(arena.getSpawnPoint(5), brawlerOf(channel, lilian));
        assertAt(arena.getSpawnPoint(6), brawlerOf(channel, lea));
    }

    @Test
    void joinedBrawlerAppearsAtItsSpawnPointAfterATick() {
        LocalChannel channel = new LocalChannel();
        int id = join(channel);
        startWith(id);

        server.tick();

        BrawlerState state = brawlerOf(channel, id);
        assertEquals(1.5f, state.x());
        assertEquals(2.5f, state.y());
    }

    @Test
    void eachPlayerGetsItsOwnBrawlerAtItsOwnSpawn() {
        LocalChannel first = new LocalChannel();
        LocalChannel second = new LocalChannel();
        int firstId = join(first);
        int secondId = join(second);
        start(firstId);

        server.tick();

        assertNotEquals(firstId, secondId);
        assertEquals(2, first.latestSnapshot().brawlers().size());
        assertEquals(4.5f, brawlerOf(first, secondId).x());
    }

    @Test
    void inputMovesTheBrawler() {
        LocalChannel channel = new LocalChannel();
        int id = join(channel);
        startWith(id);
        skipCountdown();

        channel.sendInput(new PlayerInput(1, 1, 0, 1, 0, false));
        server.tick();

        assertTrue(brawlerOf(channel, id).x() > 1.5f);
    }

    @Test
    void playerWithoutInputStaysInPlace() {
        LocalChannel channel = new LocalChannel();
        int id = join(channel);
        startWith(id);

        server.tick();
        server.tick();

        assertEquals(1.5f, brawlerOf(channel, id).x());
        assertEquals(2.5f, brawlerOf(channel, id).y());
    }

    @Test
    void inputOnlyMovesTheBrawlerOfItsSender() {
        LocalChannel first = new LocalChannel();
        LocalChannel second = new LocalChannel();
        int firstId = join(first);
        int secondId = join(second);
        start(firstId);
        skipCountdown();

        first.sendInput(new PlayerInput(1, 0, 1, 0, 1, false));
        server.tick();

        assertTrue(brawlerOf(first, firstId).y() > 2.5f);
        assertEquals(2.5f, brawlerOf(first, secondId).y());
    }

    @Test
    void everyClientReceivesTheSameSnapshot() {
        LocalChannel first = new LocalChannel();
        LocalChannel second = new LocalChannel();
        int firstId = join(first);
        join(second);
        start(firstId);

        server.tick();

        assertNotNull(first.latestSnapshot());
        assertEquals(first.latestSnapshot(), second.latestSnapshot());
    }

    @Test
    void snapshotTickIncreasesByOneAtEachTick() {
        LocalChannel channel = new LocalChannel();
        startWith(join(channel));

        server.tick();
        int first = channel.latestSnapshot().tick();
        server.tick();
        int second = channel.latestSnapshot().tick();

        assertEquals(first + 1, second);
    }

    @Test
    void snapshotAcknowledgesTheLastProcessedInput() {
        LocalChannel channel = new LocalChannel();
        int id = join(channel);
        startWith(id);

        channel.sendInput(new PlayerInput(5, 1, 0, 1, 0, false));
        server.tick();

        assertEquals(5, brawlerOf(channel, id).lastProcessedInput());
    }

    @Test
    void eachBrawlerIsAcknowledgedForItsOwnInputs() {
        LocalChannel first = new LocalChannel();
        LocalChannel second = new LocalChannel();
        int firstId = join(first);
        int secondId = join(second);
        start(firstId);

        first.sendInput(new PlayerInput(3, 0, 0, 1, 0, false));
        second.sendInput(new PlayerInput(8, 0, 0, 1, 0, false));
        server.tick();

        assertEquals(3, brawlerOf(first, firstId).lastProcessedInput());
        assertEquals(8, brawlerOf(first, secondId).lastProcessedInput());
    }

    @Test
    void leavingPlayerDisappearsFromTheSnapshot() {
        LocalChannel stayer = new LocalChannel();
        LocalChannel leaver = new LocalChannel();
        int stayerId = join(stayer);
        int leaverId = join(leaver);
        start(stayerId);

        server.leave(leaverId);
        server.tick();

        assertEquals(1, stayer.latestSnapshot().brawlers().size());
        assertEquals(stayerId, stayer.latestSnapshot().brawlers().get(0).id());
    }

    @Test
    void leavingPlayerNoLongerReceivesSnapshots() {
        LocalChannel stayer = new LocalChannel();
        LocalChannel leaver = new LocalChannel();
        int stayerId = join(stayer);
        int leaverId = join(leaver);
        start(stayerId);

        server.leave(leaverId);
        server.tick();

        assertNull(leaver.latestSnapshot());
    }

    @Test
    void leavingTwiceOrWithAnUnknownIdIsHarmless() {
        LocalChannel channel = new LocalChannel();
        int id = join(channel);
        startWith(id);

        server.leave(id);
        server.leave(id);
        server.leave(999);
        server.tick();

        assertNull(channel.latestSnapshot());
    }

    @Test
    void serverGoesBackToTheLobbyWhenEveryoneLeaves() {
        int leader = server.join(new LocalChannel(), "Louis");
        int other = server.join(new LocalChannel(), "Lilian");
        start(leader);

        server.leave(leader);
        server.leave(other);

        LocalChannel newcomer = new LocalChannel();
        server.join(newcomer, "Tom");
        assertFalse(newcomer.latestLobby().started());
    }

    @Test
    void noSnapshotIsSentBeforeTheFirstTick() {
        LocalChannel channel = new LocalChannel();

        startWith(join(channel));

        assertNull(channel.latestSnapshot());
    }

    @Test
    void projectilesNeverReuseTheIdOfABrawler() {
        LocalChannel channel = new LocalChannel();
        int id = join(channel);
        int other = join(new LocalChannel());
        start(id);
        skipCountdown();

        channel.sendInput(new PlayerInput(1, 0, 0, 1, 0, true));
        server.tick();

        assertTrue(channel.latestSnapshot().projectiles().stream().allMatch(p -> p.id() != id && p.id() != other));
    }

    @Test
    void snapshotsCarryTheMatchState() {
        LocalChannel channel = new LocalChannel();
        startWith(join(channel));

        server.tick();

        MatchState match = matchOf(channel);
        assertEquals(MatchPhase.COUNTDOWN, match.phase());
        assertEquals(1, match.round());
        assertEquals(0, match.blueScore());
        assertEquals(0, match.redScore());
    }

    @Test
    void brawlersCannotMoveDuringTheCountdown() {
        LocalChannel channel = new LocalChannel();
        int id = join(channel);
        startWith(id);

        channel.sendInput(new PlayerInput(4, 1, 0, 1, 0, false));
        tick(server, 10);

        assertEquals(1.5f, brawlerOf(channel, id).x());
        assertEquals(4, brawlerOf(channel, id).lastProcessedInput());
    }

    @Test
    void roundEndsWhenTheEnemyTeamIsEliminated() {
        LocalChannel shooter = new LocalChannel();
        startWith(join(shooter));
        skipCountdown();

        shootUntilTheRoundEnds(shooter);

        MatchState match = matchOf(shooter);
        assertEquals(MatchPhase.ROUND_OVER, match.phase());
        assertEquals(Team.BLUE, match.roundWinner());
        assertEquals(1, match.blueScore());
    }

    @Test
    void nextRoundPutsEveryoneBackAtFullHealth() {
        LocalChannel shooter = new LocalChannel();
        int shooterId = join(shooter);
        startWith(shooterId);
        skipCountdown();
        shootUntilTheRoundEnds(shooter);

        tick(server, ticksFor(KnockoutMatch.ROUND_OVER_TIME));

        MatchState match = matchOf(shooter);
        assertEquals(MatchPhase.COUNTDOWN, match.phase());
        assertEquals(2, match.round());
        WorldSnapshot snapshot = shooter.latestSnapshot();
        assertEquals(2, snapshot.brawlers().size());
        BrawlerState enemy = snapshot.brawlers().stream().filter(b -> b.id() != shooterId).findFirst().orElseThrow();
        assertEquals(4.5f, enemy.x());
        assertEquals(enemy.maxHealth(), enemy.health());
        assertTrue(snapshot.projectiles().isEmpty());
    }

    @Test
    void winningTwoRoundsSendsEveryoneBackToTheLobby() {
        LocalChannel shooter = new LocalChannel();
        startWith(join(shooter));
        skipCountdown();
        shootUntilTheRoundEnds(shooter);
        tick(server, ticksFor(KnockoutMatch.ROUND_OVER_TIME));
        skipCountdown();
        shootUntilTheRoundEnds(shooter);

        assertEquals(MatchPhase.MATCH_OVER, matchOf(shooter).phase());
        assertEquals(Team.BLUE, matchOf(shooter).matchWinner());

        tick(server, ticksFor(KnockoutMatch.MATCH_OVER_TIME));

        assertFalse(shooter.latestLobby().started());
        server.join(new LocalChannel(), "Newcomer");
    }

    @Test
    void lastPlayerOfATeamLeavingForfeitsTheMatch() {
        Arena arena = ArenaLoader.parse(TEAM_MAP);
        GameServer twoAgainstOne = new GameServer(List.of("teams"), "teams", map -> arena);
        LocalChannel blue = new LocalChannel();
        int leader = twoAgainstOne.join(blue, "Louis");
        int red = twoAgainstOne.join(new LocalChannel(), "Lilian");
        twoAgainstOne.join(new LocalChannel(), "Tom");
        twoAgainstOne.requestStart(leader);

        twoAgainstOne.leave(red);
        twoAgainstOne.tick();

        assertEquals(MatchPhase.MATCH_OVER, matchOf(blue).phase());
        assertEquals(Team.BLUE, matchOf(blue).matchWinner());

        tick(twoAgainstOne, ticksFor(KnockoutMatch.MATCH_OVER_TIME));

        assertFalse(blue.latestLobby().started());
    }
}
