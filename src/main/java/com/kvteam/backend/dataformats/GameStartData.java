package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Created by maxim on 28.03.17.
 */
public class GameStartData extends CardsForNextMoveGameServerData {
    @NotNull
    @JsonProperty("side")
    private String side;
    @NotNull
    @JsonProperty("enemyUsername")
    private String enemyUsername;
    @JsonProperty("movesCount")
    private int movesCount;
    @JsonProperty("castleMaxHP")
    private int castleMaxHP;


    public GameStartData(
            @NotNull UUID gameID,
            @NotNull String enemyUsername,
            @NotNull String side,
            int movesCount,
            int castleMaxHP,
            List<CardData> allowedCards){
        super(GameServerData.START, gameID);
        this.side = side;
        this.enemyUsername = enemyUsername;
        this.movesCount = movesCount;
        this.castleMaxHP = castleMaxHP;
    }
}
