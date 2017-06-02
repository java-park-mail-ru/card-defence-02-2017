package com.kvteam.backend.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvteam.backend.gameplay.CardManager;
import com.kvteam.backend.gameplay.GameplaySettings;
import com.kvteam.backend.websockets.IPlayerConnection;
import com.kvteam.backend.websockets.PlayerConnection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import java.sql.SQLException;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Created by maxim on 24.04.17.
 */
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@RunWith(SpringRunner.class)
@Transactional
public class MatchmakingServiceTest {
    @Mock
    private GameDbService gameDbService;

    private MatchmakingService matchmakingService;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CardManager cardManager;
    @Autowired
    private GameplaySettings settings;
    @Autowired
    private TimeoutService timeoutService;

    @Before
    public void setup() throws SQLException{
        MockitoAnnotations.initMocks(this);
        final GameService gameService = new GameService(objectMapper, gameDbService, cardManager, settings, timeoutService);

        matchmakingService = new MatchmakingService(gameService);

        when(
                gameDbService.insertNewMatch(any(), any(), anyInt(), anyInt())
        ).thenReturn(UUID.randomUUID());
        when(
                gameDbService.insertMove(
                        any(),
                        anyInt(),
                        any(),
                        any()
                )
        ).thenReturn(1);
    }


    private void testTwoPlayers(
            String side1,
            String side2,
            IPlayerConnection.ConnectionStatus expectStatus){
        final IPlayerConnection connection1 =
                new PlayerConnection("first_user", new MatchmakingTestingWebSocketSession(side1));
        final IPlayerConnection connection2 =
                new PlayerConnection("second_user", new MatchmakingTestingWebSocketSession(side2));

        matchmakingService.addPlayer(connection1, side1);
        matchmakingService.addPlayer(connection2, side2);

        Assert.assertTrue(connection1.getConnectionStatus() == expectStatus);
        Assert.assertTrue(connection2.getConnectionStatus() == expectStatus);

    }

    @Test
    public void testAllSide() throws SQLException{
        testTwoPlayers("all", "all", IPlayerConnection.ConnectionStatus.PLAYING);
    }

    @Test
    public void attackAndDefence() throws SQLException{
        testTwoPlayers("attack", "defence", IPlayerConnection.ConnectionStatus.PLAYING);
    }

    @Test
    public void defenceAndAttack() throws SQLException{
        testTwoPlayers("defence", "attack", IPlayerConnection.ConnectionStatus.PLAYING);
    }

    @Test
    public void testTwoDefence() throws SQLException{
        testTwoPlayers("defence", "defence", IPlayerConnection.ConnectionStatus.MATCHMAKING);
    }

    @Test
    public void testTwoAttack() throws SQLException{
        testTwoPlayers("attack", "attack", IPlayerConnection.ConnectionStatus.MATCHMAKING);
    }

    @Test
    public void testTwoDefenceAndAll() throws SQLException{
        testTwoPlayers("defence", "defence", IPlayerConnection.ConnectionStatus.MATCHMAKING);
        final IPlayerConnection connection =
                new PlayerConnection("third", new MatchmakingTestingWebSocketSession("all"));

        matchmakingService.addPlayer(connection, "all");

        Assert.assertTrue(connection.getConnectionStatus() == IPlayerConnection.ConnectionStatus.PLAYING);
    }

    @Test
    public void testTwoAttackAndAll() throws SQLException{
        testTwoPlayers("attack", "attack", IPlayerConnection.ConnectionStatus.MATCHMAKING);
        final IPlayerConnection connection =
                new PlayerConnection("third", new MatchmakingTestingWebSocketSession("all"));

        matchmakingService.addPlayer(connection, "all");

        Assert.assertTrue(connection.getConnectionStatus() == IPlayerConnection.ConnectionStatus.PLAYING);
    }


}
