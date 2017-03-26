package com.kvteam.backend.websockets;

import com.kvteam.backend.services.MatchmakingService;
import com.kvteam.backend.services.SingleplayerHubService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by maxim on 14.03.17.
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private MatchmakingService matchmakingService;
    private SingleplayerHubService singleplayerHubService;
    private Map<String, IPlayerConnection> connections =
            new HashMap<>();

    public GameWebSocketHandler(
            MatchmakingService matchmakingService,
            SingleplayerHubService singleplayerHubService){
        this.matchmakingService = matchmakingService;
        this.singleplayerHubService = singleplayerHubService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        final String type = session
                .getAttributes()
                .getOrDefault(CheckCredentialsWebsocketInterceptor.TYPE_PARAM, "")
                .toString();
        final String side = session
                .getAttributes()
                .getOrDefault(CheckCredentialsWebsocketInterceptor.SIDE_PARAM, "")
                .toString();
        final IPlayerConnection connection = new PlayerConnection(
                session.getAttributes().get(CheckCredentialsWebsocketInterceptor.USERNAME_PARAM).toString(),
                session
        );
        connections.put(
                session.getId(),
                connection
        );
        if (type.equals(CheckCredentialsWebsocketInterceptor.SINGLEPLAYER)){
            singleplayerHubService.addPlayer(connection);
        } else if(type.equals(CheckCredentialsWebsocketInterceptor.MULTIPLAYER)) {
            matchmakingService.addPlayer(connection, side);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status){
        final IPlayerConnection connection = connections.remove(session.getId());
        if (connection != null) {
            connection.notifyOnClose(status);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws IOException{
        final IPlayerConnection connection = connections.get(session.getId());
        if (connection != null) {
            connection.notifyOnReceive(message.getPayload());
        }
    }
}
