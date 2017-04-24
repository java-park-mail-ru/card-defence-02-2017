package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by maxim on 28.03.17.
 */
// Отправляется только сервер -> клиент
public class ActionData {
    @NotNull
    @JsonProperty("unitID")
    private UUID unitID;
    @NotNull
    @JsonProperty("actiontType")
    private String actionType;
    @NotNull
    @JsonProperty("actionParameters")
    private Map<String, Object> actionParameters;
    @JsonProperty("timeOffsetBegin")
    private int timeOffsetBegin;
    @JsonProperty("timeOffsetEnd")
    private int timeOffsetEnd;

    @JsonCreator
    public ActionData(@JsonProperty("unitID") @NotNull UUID unitID,
                      @JsonProperty("actionType") String actionType,
                      @JsonProperty("actionParameters") @NotNull Map<String, Object> actionParameters,
                      @JsonProperty("timeOffsetBegin") int timeOffsetBegin,
                      @JsonProperty("timeOffsetEnd") int timeOffsetEnd){
        this.unitID = unitID;
        this.actionType = actionType;
        this.actionParameters = actionParameters;
        this.timeOffsetBegin = timeOffsetBegin;
        this.timeOffsetEnd = timeOffsetEnd;
    }

}
