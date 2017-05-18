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
import org.springframework.web.socket.WebSocketSession;

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
public class SingleplayerHubServiceTest {
    @Mock
    private GameDbService gameDbService;

    private SingleplayerHubService hubService;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CardManager cardManager;
    @Autowired
    private GameplaySettings settings;

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);
        final GameService gameService = new GameService(objectMapper, gameDbService, cardManager, settings);
        hubService = new SingleplayerHubService(gameService);
    }

    private void testTwoUsers(
            String firstUser,
            String secondUser,
            IPlayerConnection.ConnectionStatus status) throws SQLException{
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
        final WebSocketSession session = new MatchmakingTestingWebSocketSession(null);
        final WebSocketSession session1 = new MatchmakingTestingWebSocketSession(null);
        final IPlayerConnection connection = new PlayerConnection(
                firstUser, session

        );
        final IPlayerConnection connection1 = new PlayerConnection(
                secondUser, session1

        );
        hubService.addPlayer(connection);
        hubService.addPlayer(connection1);

        Assert.assertTrue(connection.getConnectionStatus() == status);
        Assert.assertTrue(connection.getConnectionStatus() == status);
    }

    @Test
    public void connectSameName() throws SQLException{
        testTwoUsers("nagibator", "nagibator", IPlayerConnection.ConnectionStatus.PLAYING);
    }

    @Test
    public void differentNames() throws SQLException{
        testTwoUsers("user1", "user2", IPlayerConnection.ConnectionStatus.MATCHMAKING);
    }
}
