package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Created by maxim on 05.04.17.
 */
public class MoveResultGameServerData extends GameServerData {
    @JsonProperty("currentMove")
    private int currentMove;
    @JsonProperty("castleHP")
    private int castleHP; // В начале хода
    @JsonProperty("myUnits")
    private List<UnitData> myUnits;
    @JsonProperty("enemyUnits")
    private List<UnitData> enemyUnits;
    @JsonProperty("actions")
    private List<ActionData> actions;

    @JsonCreator
    protected MoveResultGameServerData(
            @JsonProperty("status") @NotNull String status,
            @JsonProperty("gameID") @NotNull UUID gameID,
            @JsonProperty("currentMove") int currentMove,
            @JsonProperty("castleHP") int castleHP,
            @JsonProperty("myUnits") List<UnitData> myUnits,
            @JsonProperty("enemyUnits") List<UnitData> enemyUnits,
            @JsonProperty("actions") List<ActionData> actions) {
        super(status, gameID);
        this.currentMove = currentMove;
        this.castleHP = castleHP;
        this.myUnits = myUnits;
        this.enemyUnits = enemyUnits;
        this.actions = actions;
    }

    public int getCurrentMove(){
        return currentMove;
    }

    public List<UnitData> getMyUnits(){
        return myUnits;
    }

    public List<UnitData> getEnemyUnits(){
        return enemyUnits;
    }

    public List<ActionData> getActions(){
        return actions;
    }
}
