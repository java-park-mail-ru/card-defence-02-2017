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
public class GameClientData extends GameStatusData{
    public static final String READY = "ready";
    public static final String RENDER_COMPLETE = "render_complete";
    public static final String SEND_CHAT_MESSAGE = "send_chat_message";
    public static final String CLOSE = "close";

    @JsonCreator
    public GameClientData(
            @JsonProperty("status") @Nullable String status,
            @JsonProperty("gameID") @NotNull UUID gameID) {
        super(status, gameID);
    }
}
