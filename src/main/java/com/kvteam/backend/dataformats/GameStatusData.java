package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Created by maxim on 28.03.17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class GameStatusData {
    public static final String UNDEFINED = "undefined";

    @NotNull
    @JsonProperty("status")
    private String status = "undefined";
    @NotNull
    @JsonProperty("gameID")
    private UUID gameID ;

    public GameStatusData(@Nullable String status, @NotNull UUID gameID) {
        this.status = status != null ? status : UNDEFINED;
        this.gameID = gameID;
    }

    @NotNull
    public String getStatus(){
        return status;
    }

    @NotNull
    public UUID getGameID() {
        return gameID;
    }
}
