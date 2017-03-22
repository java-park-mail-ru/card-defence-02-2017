package com.kvteam.backend;

import com.kvteam.backend.services.MatchmakingService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by maxim on 14.03.17.
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private MatchmakingService matchmakingService;

    private List<WebSocketSession> sessions = new ArrayList<>();

    public GameWebSocketHandler(MatchmakingService matchmakingService){
        this.matchmakingService = matchmakingService;
    }

    // This will send only to one client(most recently connected)
    public void counterIncrementedCallback(int counter) {
        System.out.println("Trying to send:" + counter);
        for (WebSocketSession session: sessions) {
            if (session != null && session.isOpen()) {
                try {
                    System.out.println("Now sending:" + counter);
                    session.sendMessage(new TextMessage("{\"value\": \"" + counter + "\"}"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Don't have open session to send:" + counter);
            }
        }

    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("User " + session.getAttributes().get("username") + " connected");
        final String type = session
                .getAttributes()
                .getOrDefault("type", "")
                .toString();
        if (type.equals("singleplayer")){
            // стартуем синглплеер
        } else if(type.equals("multiplayer")) {
            matchmakingService.addPlayer(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status){
        System.out.println("Connection closed with status " + status.getReason());
        matchmakingService.notifyPlayerDisconnected(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws IOException{
        if ("CLOSE".equalsIgnoreCase(message.getPayload())) {
            session.close();
        } else {
            System.out.println("Received:" + message.getPayload());
        }
    }
}
