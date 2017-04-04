package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Created by maxim on 01.04.17.
 */
public class GameFinishingServerData extends GameServerData {
    @JsonProperty("currentMove")
    private int currentMove;
    @JsonProperty("castleHP")
    private int castleHP; // В начале хода
    @JsonProperty("units")
    private List<UnitData> units;
    @JsonProperty("actions")
    private List<ActionData> actions;

    public GameFinishingServerData(
            @NotNull UUID gameID,
            int currentMove,
            int castleHP,
            boolean attackerIsWin,
            List<UnitData> units,
            List<ActionData> actions) {
        super(attackerIsWin ?
                GameServerData.ATTACK_WIN :
                GameServerData.DEFENCE_WIN,
              gameID);
        this.currentMove = currentMove;
        this.castleHP = castleHP;
        this.units = units;
        this.actions = actions;
    }

    public int getCurrentMove(){
        return currentMove;
    }

    public int getCastleHP(){
        return castleHP;
    }

    public List<UnitData> getUnits(){
        return units;
    }

    public List<ActionData> getActions(){
        return actions;
    }
}
