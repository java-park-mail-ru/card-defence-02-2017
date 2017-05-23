package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

/**
 * Created by maxim on 28.03.17.
 */
public class CardData {
    @NotNull
    @JsonProperty("alias")
    private String alias;

    @JsonCreator
    public CardData(
            @JsonProperty("alias") @NotNull String alias) {
        this.alias = alias;
    }

    @NotNull
    public String getAlias(){
        return alias;
    }
}
