package com.kvteam.backend.gameplay;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by maxim on 04.04.17.
 */
public class GameplaySettings {
    private int maxMovesCount;
    private int maxCastleHP;

    @JsonCreator
    public GameplaySettings(
            @JsonProperty("maxMovesCount") int maxMovesCount,
            @JsonProperty("maxCastleHP") int maxCastleHP){
        this.maxMovesCount = maxMovesCount;
        this.maxCastleHP = maxCastleHP;
    }

    public int getMaxMovesCount(){
        return maxMovesCount;
    }

    public int getMaxCastleHP(){
        return maxCastleHP;
    }
}
