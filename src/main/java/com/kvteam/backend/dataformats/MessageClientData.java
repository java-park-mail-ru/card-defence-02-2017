package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Created by maxim on 28.03.17.
 */
public class MessageClientData extends GameClientData {
    @NotNull
    @JsonProperty("message")
    private String message;

    @JsonCreator
    public MessageClientData(
            @JsonProperty("gameID") @NotNull UUID gameID,
            @JsonProperty("message") @NotNull String message){
        super(GameClientData.SEND_CHAT_MESSAGE, gameID);
        this.message = message;
    }

    @NotNull
    public String getMessage(){
        return message;
    }
}
