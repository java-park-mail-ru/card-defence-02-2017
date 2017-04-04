package com.kvteam.backend.gameplay;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by maxim on 04.04.17.
 */
public class Action {
    @NotNull
    private Unit actor;
    @NotNull
    private ActionType type;
    @NotNull
    private Map<String, Object> actionParams;
    private int beginOffset;
    private int endOffset;

    public Action(
            @NotNull Unit actor,
            @NotNull ActionType type){
        this.actor = actor;
        this.type = type;
        this.actionParams = new HashMap<>();
        this.beginOffset = -1;
        this.endOffset = -1;
    }

    public Action(
            @NotNull Unit actor,
            @NotNull ActionType type,
            @NotNull Map<String, Object> actionParams,
            int beginOffset,
            int endOffset){
        this.actor = actor;
        this.type = type;
        this.actionParams = actionParams;
        this.beginOffset = beginOffset;
        this.endOffset = endOffset;
    }

    @NotNull
    public Unit getActor(){
        return actor;
    }

    @NotNull
    public ActionType getActionType(){
        return type;
    }

    @NotNull
    public Map<String, Object> getActionParams(){
        return actionParams;
    }

    public void addActionParam(@NotNull String key, @NotNull Object value){
        actionParams.put(key, value);
    }

    public int getBeginOffset(){
        return beginOffset;
    }

    public void setBeginOffset(int offset){
        beginOffset = offset;
    }

    public int getEndOffset(){
        return endOffset;
    }

    public void setEndOffset(int offset){
        endOffset = offset;
    }
}
