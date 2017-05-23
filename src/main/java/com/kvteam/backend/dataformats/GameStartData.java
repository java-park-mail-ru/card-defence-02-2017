package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonCreator;
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
    @JsonProperty("timeout")
    private int timeout;

    @JsonCreator
    public GameStartData(
            @JsonProperty("gameID") @NotNull UUID gameID,
            @JsonProperty("enemyUsername") @NotNull String enemyUsername,
            @JsonProperty("side") @NotNull String side,
            @JsonProperty("movesCount") int movesCount,
            @JsonProperty("castleMaxHP") int castleMaxHP,
            @JsonProperty("timeout") int timeout,
            @JsonProperty("allowedCards") List<CardData> allowedCards){
        super(GameServerData.START, gameID, allowedCards);
        this.side = side;
        this.enemyUsername = enemyUsername;
        this.movesCount = movesCount;
        this.castleMaxHP = castleMaxHP;
        this.timeout = timeout;
    }
}
