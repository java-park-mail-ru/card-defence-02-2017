package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Created by maxim on 05.04.17.
 */
public class CardsForNextMoveGameServerData extends GameServerData {
    @JsonProperty("allowedCards")
    private List<CardData> allowedCards;

    public CardsForNextMoveGameServerData(
            @NotNull UUID gameID,
            @NotNull List<CardData> allowedCards) {
        super(GameServerData.AVAILABLE_CARDS, gameID);
        this.allowedCards = allowedCards;
    }

    protected CardsForNextMoveGameServerData(
            @NotNull String status,
            @NotNull UUID gameID,
            @NotNull List<CardData> allowedCards) {
        super(status, gameID);
        this.allowedCards = allowedCards;
    }
}
