package com.kvteam.backend.services;

import com.kvteam.backend.websockets.IPlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.validation.constraints.Null;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by maxim on 15.03.17.
 */
@Service
public class MatchmakingService {
    public enum Role{
        ATTACK,
        DEFENCE,
        ALL
    };

    private Queue<IPlayerConnection> defencePlayers =
            new ConcurrentLinkedQueue<>();

    private Queue<IPlayerConnection> attackPlayers =
            new ConcurrentLinkedQueue<>();

    private Queue<IPlayerConnection> anyTeamPlayers =
            new ConcurrentLinkedQueue<>();

    private GameService gameService;

    public MatchmakingService(GameService gameService){
        this.gameService = gameService;
    }

    @Nullable
    private IPlayerConnection getFirstOpenPlayer(Queue<IPlayerConnection> players){
        IPlayerConnection conn;
        do{
            conn = players.poll();
        }while(conn != null && conn.isClosed());
        return conn;
    }

    public synchronized void addPlayer(
            @NotNull IPlayerConnection connection,
            @NotNull String side){
        connection.markAsMatchmaking();
        if(side.equals("attack")) {
            final IPlayerConnection player = getFirstOpenPlayer(defencePlayers);
            if (player != null) {
                gameService.startGame(connection, player);
            } else {
                connection.onClose((playerConnection, status) -> System.out.println("Передумал " + playerConnection.getUsername()) );
                attackPlayers.add(connection);
            }
        }else if(side.equals("defence")){
            final IPlayerConnection player = getFirstOpenPlayer(attackPlayers);
            if(player != null){
                gameService.startGame(player, connection);
            }else{
                connection.onClose((playerConnection, status) -> System.out.println("Передумал " + playerConnection.getUsername()) );
                defencePlayers.add(connection);
            }
        }else if(side.equals("all")){
            final IPlayerConnection player = getFirstOpenPlayer(anyTeamPlayers);
            if(player != null){
                gameService.startGame(player, connection);
            }else{
                connection.onClose((playerConnection, status) -> System.out.println("Передумал " + playerConnection.getUsername()) );
                anyTeamPlayers.add(connection);
            }
        }else{
            connection.markAsErrorable();
        }
    }

    // Закрытые сокеты - заблудшие души в матчмейкере
    @Scheduled(fixedDelay = 1000 * 30)
    void deleteLostConnections(){
        attackPlayers.removeIf(IPlayerConnection::isClosed);
        defencePlayers.removeIf(IPlayerConnection::isClosed);
        anyTeamPlayers.removeIf(IPlayerConnection::isClosed);
    }
}
