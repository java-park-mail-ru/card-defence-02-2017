package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Created by maxim on 01.04.17.
 */
public class ContinousGameServerData extends GameServerData {
    @JsonProperty("currentMove")
    private int currentMove;
    @JsonProperty("castleHP")
    private int castleHP;
    @JsonProperty("allowedCards")
    private List<CardData> allowedCards;
    @JsonProperty("units")
    private List<UnitData> units;
    @JsonProperty("actions")
    private List<ActionData> actions;

    public ContinousGameServerData(
            @NotNull UUID gameID,
            int currentMove,
            int castleHP,
            List<CardData> allowedCards,
            List<UnitData> units,
            List<ActionData> actions) {
        super(GameServerData.CONTINOUS, gameID);
        this.currentMove = currentMove;
        this.castleHP = castleHP;
        this.allowedCards = allowedCards;
        this.units = units;
        this.actions = actions;
    }
}
