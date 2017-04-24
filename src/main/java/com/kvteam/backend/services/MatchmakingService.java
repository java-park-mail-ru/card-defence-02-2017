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

    @Nullable
    private IPlayerConnection getFirstOpenPlayer(String side) throws Exception{
        IPlayerConnection conn;
        if(side.equals("all")){
            conn = getFirstOpenPlayer(anyTeamPlayers);
            if(conn == null){
                conn = getFirstOpenPlayer(defencePlayers);
            }
            if(conn == null){
                conn = getFirstOpenPlayer(attackPlayers);
            }
        }else if(side.equals("attack")){
            conn = getFirstOpenPlayer(defencePlayers);
            if(conn == null){
                conn = getFirstOpenPlayer(anyTeamPlayers);
            }
        }else if(side.equals("defence")){
            conn = getFirstOpenPlayer(attackPlayers);
            if(conn == null){
                conn = getFirstOpenPlayer(anyTeamPlayers);
            }
        } else {
            throw new Exception("Wrong side");
        }
        return conn;
    }

    public Queue<IPlayerConnection> getSamePlayers(String side) throws Exception{
        if(side.equals("all")){
            return anyTeamPlayers;
        }else if(side.equals("attack")){
            return attackPlayers;
        }else if(side.equals("defence")){
            return defencePlayers;
        } else {
            throw new Exception("Wrong side");
        }
    }

    public synchronized void addPlayer(
            @NotNull IPlayerConnection connection,
            @NotNull String side){
        connection.markAsMatchmaking();
        try{
            processPlayer(connection, side);
        }catch(Exception e){
            connection.markAsErrorable();
        }
    }

    @SuppressWarnings("Duplicates")
    private void processPlayer(
            IPlayerConnection connection,
            String side) throws Exception{
        final IPlayerConnection player = getFirstOpenPlayer(side);
        if(player != null){
            connection.onClose(null);
            connection.onReceive(null);
            player.onClose(null);
            player.onReceive(null);
            gameService.startGame(player, connection);
        } else {
            final Queue<IPlayerConnection> samePlayers = getSamePlayers(side);
            connection.onClose((playerConnection, status) -> samePlayers.remove(playerConnection));
            connection.onReceive(null);
            samePlayers.add(connection);
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
