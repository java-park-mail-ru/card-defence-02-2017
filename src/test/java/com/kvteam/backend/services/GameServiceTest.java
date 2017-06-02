package com.kvteam.backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvteam.backend.dataformats.*;
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Created by maxim on 24.04.17.
 */
@SuppressWarnings({"SpringJavaAutowiredMembersInspection", "Duplicates", "OverlyBroadThrowsClause"})
@SpringBootTest(webEnvironment = RANDOM_PORT)
@RunWith(SpringRunner.class)
@Transactional
public class GameServiceTest {
    @Mock
    private GameDbService gameDbService;

    private GameService gameService;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CardManager cardManager;
    @Autowired
    private GameplaySettings settings;
    @Autowired
    private TimeoutService timeoutService;

    @Before
    public void setup() throws SQLException {
        MockitoAnnotations.initMocks(this);
        gameService = new GameService(objectMapper, gameDbService, cardManager, settings, timeoutService);

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

    @Test
    public void startGame() throws IOException{
        final GameTestWebSocketSession session1 =  new GameTestWebSocketSession();
        final GameTestWebSocketSession session2 = new GameTestWebSocketSession();
        final IPlayerConnection connection1 =
                new PlayerConnection("first_user", session1);
        final IPlayerConnection connection2 =
                new PlayerConnection("second_user", session2);
        connection1.markAsMatchmaking();
        connection2.markAsMatchmaking();
        gameService.startGame(connection1, connection2);
        Assert.assertTrue(connection1.getConnectionStatus() == IPlayerConnection.ConnectionStatus.PLAYING);
        Assert.assertTrue(connection2.getConnectionStatus() == IPlayerConnection.ConnectionStatus.PLAYING);

        final GameStartData msg1 = objectMapper.readValue(session1.getLastReceivedString(), GameStartData.class);
        final GameStartData msg2 = objectMapper.readValue(session2.getLastReceivedString(), GameStartData.class);
        Assert.assertEquals(msg1.getGameID(), msg2.getGameID());
        Assert.assertEquals(msg1.getStatus(), GameServerData.START);
        Assert.assertEquals(msg2.getStatus(), GameServerData.START);
    }

    @Test
    public void playFirstMove() throws IOException{
        final GameTestWebSocketSession session1 =  new GameTestWebSocketSession();
        final GameTestWebSocketSession session2 = new GameTestWebSocketSession();
        final IPlayerConnection connection1 =
                new PlayerConnection("first_user", session1);
        final IPlayerConnection connection2 =
                new PlayerConnection("second_user", session2);
        connection1.markAsMatchmaking();
        connection2.markAsMatchmaking();
        gameService.startGame(connection1, connection2);
        Assert.assertTrue(connection1.getConnectionStatus() == IPlayerConnection.ConnectionStatus.PLAYING);
        Assert.assertTrue(connection2.getConnectionStatus() == IPlayerConnection.ConnectionStatus.PLAYING);

        final GameStartData msg1 = objectMapper.readValue(session1.getLastReceivedString(), GameStartData.class);
        final GameStartData msg2 = objectMapper.readValue(session2.getLastReceivedString(), GameStartData.class);
        Assert.assertEquals(msg1.getGameID(), msg2.getGameID());
        Assert.assertEquals(msg1.getStatus(), GameServerData.START);
        Assert.assertEquals(msg2.getStatus(), GameServerData.START);

        final List<PositionedCardData> attackCards =
                new ArrayList<>();
        attackCards.add(new PositionedCardData(msg1.getAllowedCards().get(0).getAlias(),
                new PointData(0, 0)));
        attackCards.add(new PositionedCardData(msg2.getAllowedCards().get(0).getAlias(),
                new PointData(1, 0)));
        final ReadyClientData readyClient1 = new ReadyClientData(
                GameClientData.READY,
                msg1.getGameID(),
                attackCards);
        final List<PositionedCardData> defence = new ArrayList<>();
        final ReadyClientData readyClient2 = new ReadyClientData(
                GameClientData.READY,
                msg2.getGameID(),
                defence);

        connection1.notifyOnReceive(objectMapper.writeValueAsString(readyClient1));
        connection2.notifyOnReceive(objectMapper.writeValueAsString(readyClient2));

        final MoveResultGameServerData msg3 =
                objectMapper.readValue(session1.getLastReceivedString(), MoveResultGameServerData.class);
        final MoveResultGameServerData msg4 =
                objectMapper.readValue(session2.getLastReceivedString(), MoveResultGameServerData.class);
        Assert.assertTrue(msg3.getCurrentMove() == 1);
        Assert.assertTrue(msg4.getCurrentMove() == 1);
        Assert.assertEquals(msg3.getActions().size(), msg4.getActions().size());
        Assert.assertEquals(msg3.getMyUnits().size(), msg4.getEnemyUnits().size());
        Assert.assertEquals(msg4.getMyUnits().size(), msg3.getEnemyUnits().size());

        //noinspection ConstantConditions тут null быть не должно, так что если исключение - надо разбираться
        final RenderCompleteClientData rc1 = new RenderCompleteClientData(connection1.getGameID());
        //noinspection ConstantConditions тут null быть не должно, так что если исключение - надо разбираться
        final RenderCompleteClientData rc2 = new RenderCompleteClientData(connection2.getGameID());

        connection1.notifyOnReceive(objectMapper.writeValueAsString(rc1));
        connection2.notifyOnReceive(objectMapper.writeValueAsString(rc2));
    }
}
