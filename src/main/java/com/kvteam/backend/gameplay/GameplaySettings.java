package com.kvteam.backend.gameplay;

/**
 * Created by maxim on 04.04.17.
 */
public class GameplaySettings {
    private int maxMovesCount;
    private int maxCastleHP;

    public GameplaySettings(int maxMovesCount, int maxCastleHP){
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
