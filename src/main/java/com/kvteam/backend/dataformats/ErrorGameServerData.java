package com.kvteam.backend.dataformats;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Created by maxim on 05.04.17.
 */
public class ErrorGameServerData extends GameServerData {
    public ErrorGameServerData(
            @NotNull UUID gameID) {
        super(GameServerData.ERROR, gameID);
    }
}
