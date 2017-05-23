package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Created by maxim on 23.05.17.
 */
public class TimeoutServerData extends GameServerData {
    @JsonProperty("timeoutFor")
    private String timeoutFor;
    @JsonProperty("winner")
    private String winner;

    public TimeoutServerData(
            @NotNull UUID gameID,
            @NotNull String timeoutFor,
            @NotNull String winner) {
        super(GameServerData.TIMEOUT, gameID);
        this.timeoutFor = timeoutFor;
        this.winner = winner;
    }
}