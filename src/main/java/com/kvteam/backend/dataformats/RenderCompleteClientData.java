package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Created by maxim on 01.04.17.
 */
public class RenderCompleteClientData extends GameClientData {
    @JsonCreator
    public RenderCompleteClientData(
            @JsonProperty("gameID") @NotNull UUID gameID) {
        super(GameClientData.READY, gameID);
    }
}
