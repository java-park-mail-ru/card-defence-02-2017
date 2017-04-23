package com.kvteam.backend.services;

import com.kvteam.backend.websockets.IPlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by maxim on 15.03.17.
 */
@Service
public class MatchmakingService {
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
            processPlayer(connection, attackPlayers);
        }else if(side.equals("defence")){
            processPlayer(connection, defencePlayers);
        }else if(side.equals("all")){
            processPlayer(connection, anyTeamPlayers);
        }else{
            connection.markAsErrorable();
        }
    }

    @SuppressWarnings("Duplicates")
    private void processPlayer(IPlayerConnection connection, Queue<IPlayerConnection> otherPlayers){
        final IPlayerConnection player = getFirstOpenPlayer(otherPlayers);
        if(player != null){
            connection.onClose(null);
            connection.onReceive(null);
            player.onClose(null);
            player.onReceive(null);
            gameService.startGame(player, connection);
        } else {
            connection.onClose((playerConnection, status) -> otherPlayers.remove(playerConnection));
            connection.onReceive(null);
            otherPlayers.add(connection);
        }
    }

    // Закрытые сокеты - заблудшие души в матчмейкере
    @Scheduled(fixedDelay = 1000 * 60)
    private void deleteLostConnections(){
        attackPlayers.removeIf(IPlayerConnection::isClosed);
        defencePlayers.removeIf(IPlayerConnection::isClosed);
        anyTeamPlayers.removeIf(IPlayerConnection::isClosed);
    }
}
