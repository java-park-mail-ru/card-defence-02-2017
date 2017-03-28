package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Created by maxim on 28.03.17.
 */
public class GameStartData extends GameServerData {
    @NotNull
    @JsonProperty("enemyUsername")
    private String enemyUsername;
    @JsonProperty("movesCount")
    private int movesCount;
    @JsonProperty("castleMaxHP")
    private int castleMaxHP;
    @JsonProperty("allowedCards")
    private CardData[] allowedCards;


    public GameStartData(
            @NotNull UUID gameID,
            @NotNull String enemyUsername,
            int movesCount,
            int castleMaxHP,
            CardData[] allowedCards){
        super(GameServerData.START, gameID);
        this.enemyUsername = enemyUsername;
        this.movesCount = movesCount;
        this.castleMaxHP = castleMaxHP;
        this.allowedCards = allowedCards;
    }
}
