package com.kvteam.backend.services;

import com.kvteam.backend.websockets.IPlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

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

    private List<IPlayerConnection> defencePlayers =
            new LinkedList<>();

    private List<IPlayerConnection> attackPlayers =
            new LinkedList<>();

    private List<IPlayerConnection> anyTeamPlayers =
            new LinkedList<>();

    private GameService gameService;

    public MatchmakingService(GameService gameService){
        this.gameService = gameService;
    }

    @Nullable
    private IPlayerConnection getDefencePlayer(){
        return !defencePlayers.isEmpty() ?
                defencePlayers.remove(0) :
                null;
    }

    @Nullable
    private IPlayerConnection getAttackPlayer(){
        return !attackPlayers.isEmpty() ?
                attackPlayers.remove(0) :
                null;
    }

    @Nullable
    private IPlayerConnection getAnyPlayer(){
        return !anyTeamPlayers.isEmpty() ?
                anyTeamPlayers.remove(0) :
                null;
    }

    public synchronized void addPlayer(
            @NotNull IPlayerConnection connection,
            @NotNull String side){
        if(side.equals("attack")) {
            final IPlayerConnection player = getDefencePlayer();
            if (player != null) {
                gameService.startGame(connection, player);
            } else {
                connection.onClose((playerConnection, status) -> System.out.println("Передумал " + playerConnection.getUsername()) );
                attackPlayers.add(connection);
            }
        }else if(side.equals("defence")){
            final IPlayerConnection player = getAttackPlayer();
            if(player != null){
                gameService.startGame(player, connection);
            }else{
                connection.onClose((playerConnection, status) -> System.out.println("Передумал " + playerConnection.getUsername()) );
                defencePlayers.add(connection);
            }
        }else if(side.equals("all")){
            final IPlayerConnection player = getAnyPlayer();
            if(player != null){
                gameService.startGame(player, connection);
            }else{
                connection.onClose((playerConnection, status) -> System.out.println("Передумал " + playerConnection.getUsername()) );
                anyTeamPlayers.add(connection);
            }
        }
    }

}
