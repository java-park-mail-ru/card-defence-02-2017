package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Created by maxim on 01.04.17.
 */
public class ReadyClientData extends GameClientData {
    @NotNull
    @JsonProperty("cards")
    private List<PositionedCardData> cards;

    @JsonCreator
    public ReadyClientData(
            @JsonProperty("status") @Nullable String status,
            @JsonProperty("gameID") @NotNull UUID gameID,
            @JsonProperty("cards") @NotNull List<PositionedCardData> cards) {
        super(status, gameID);
        this.cards = cards;
    }

    @NotNull
    public List<PositionedCardData> getCards(){
        return cards;
    }

}
