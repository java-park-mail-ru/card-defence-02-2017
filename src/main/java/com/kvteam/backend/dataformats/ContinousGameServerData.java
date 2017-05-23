package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Created by maxim on 01.04.17.
 */
public class ContinousGameServerData extends MoveResultGameServerData {
    public ContinousGameServerData(
            @NotNull UUID gameID,
            int currentMove,
            int castleHP,
            List<UnitData> myUnits,
            List<UnitData> enemyUnits,
            List<ActionData> actions) {
        super(GameServerData.CONTINOUS,
                gameID,
                currentMove,
                castleHP,
                myUnits,
                enemyUnits,
                actions);
    }
}
