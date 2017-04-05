package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Created by maxim on 01.04.17.
 */
public class GameFinishingServerData extends MoveResultGameServerData {
    public GameFinishingServerData(
            @NotNull UUID gameID,
            int currentMove,
            int castleHP,
            boolean attackerIsWin,
            List<UnitData> units,
            List<ActionData> actions) {
        super(attackerIsWin ?
                GameServerData.ATTACK_WIN :
                GameServerData.DEFENCE_WIN,
              gameID,
              currentMove,
              castleHP,
              units,
              actions);
    }
}
