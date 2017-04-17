package com.kvteam.backend.services;

import com.kvteam.backend.websockets.IPlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by maxim on 26.03.17.
 */
@Service
public class SingleplayerHubService {
    private GameService gameService;
    private Map<String, IPlayerConnection> firstConnections =
            new ConcurrentHashMap<>();

    public SingleplayerHubService(GameService gameService){
        this.gameService = gameService;
    }

    public synchronized void addPlayer(
            @NotNull IPlayerConnection playerConnection){
        playerConnection.markAsMatchmaking();
        final IPlayerConnection connection =
                firstConnections.remove(playerConnection.getUsername());
        // Если в очереди уже есть такое соединение и оно
        // по каким либо причинам не было закрыто, то начинаем игру
        // Иначе добавляем текущее соединение в ожидание второго.
        if(connection != null && !connection.isClosed()){
            gameService.startGame(connection, playerConnection);
        } else {
            firstConnections.put(
                    playerConnection.getUsername(),
                    playerConnection
            );
        }
    }

    @Scheduled(fixedDelay = 1000)
    void deleteLostConnections(){
        firstConnections.entrySet().removeIf(p -> p.getValue().isClosed());
    }
}
