package com.kvteam.backend.services;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

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

    private List<WebSocketSession> defencePlayers =
            new LinkedList<>();

    private List<WebSocketSession> attackPlayers =
            new LinkedList<>();



    @Nullable
    private WebSocketSession getDefencePlayer(){
        return !defencePlayers.isEmpty() ?
                defencePlayers.remove(0) :
                null;
    }

    @Nullable
    private WebSocketSession getAttackPlayer(){
        return !attackPlayers.isEmpty() ?
                attackPlayers.remove(0) :
                null;
    }

    public void addPlayer(
            @NotNull WebSocketSession session){
        /*if(role == Role.ATTACK
                || role == Role.ALL){
            final WebSocketSession player = getDefencePlayer();
            if(player != null){
                // session и player - два противника
            }else{
                attackPlayers.add(session);
            }
        }
        if(role == Role.DEFENCE
                || role == Role.ALL){
            final WebSocketSession player = getAttackPlayer();
            if(player != null){
                // session и player - два противника
            }else{
                defencePlayers.add(session);
            }
        }*/
    }


    public void notifyPlayerDisconnected(@NotNull WebSocketSession session){

    }

}
