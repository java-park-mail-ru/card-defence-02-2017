package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Created by maxim on 28.03.17.
 */
public class MessageServerData extends GameServerData{
    @NotNull
    @JsonProperty("sender")
    private String sender;
    @NotNull
    @JsonProperty("message")
    private String message;

    @JsonCreator
    public MessageServerData(
            @JsonProperty("gameID") @NotNull UUID gameID,
            @JsonProperty("sender") @NotNull String sender,
            @JsonProperty("message") @NotNull String message){
        super(GameServerData.SEND_CHAT_MESSAGE, gameID);
        this.sender = sender;
        this.message = message;
    }

    @NotNull
    private String getSender(){
        return sender;
    }

    @NotNull
    private String getMessage(){
        return message;
    }
}
