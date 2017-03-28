package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Created by maxim on 28.03.17.
 */
public class GameServerData extends GameStatusData {
    public static final String START = "start";
    public static final String CONTINOUS = "continous";
    public static final String DEFENCE_WIN = "defence_win";
    public static final String ATTACK_WIN = "attack_win";
    public static final String SEND_CHAT_MESSAGE = "send_chat_message";

    @JsonCreator
    public GameServerData(
            @JsonProperty("status") @Nullable String status,
            @JsonProperty("gameID") @NotNull UUID gameID) {
        super(status, gameID);
    }
}
