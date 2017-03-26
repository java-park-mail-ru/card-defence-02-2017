package com.kvteam.backend.services;

import com.kvteam.backend.websockets.IPlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by maxim on 26.03.17.
 */
@Service
public class SingleplayerHubService {
    private GameService gameService;
    private Map<String, IPlayerConnection> firstConnections =
            new HashMap<>();

    public SingleplayerHubService(GameService gameService){
        this.gameService = gameService;
    }

    public synchronized void addPlayer(
            @NotNull IPlayerConnection playerConnection){
        final IPlayerConnection connection =
                firstConnections.remove(playerConnection.getUsername());
        if(connection != null){
            gameService.startGame(connection, playerConnection);
        } else {
            firstConnections.put(
                    playerConnection.getUsername(),
                    playerConnection
            );
        }

    }
}
