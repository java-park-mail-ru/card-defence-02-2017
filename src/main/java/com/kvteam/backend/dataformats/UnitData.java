package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Created by maxim on 28.03.17.
 */
// Отправляется только сервер -> клиент
public class UnitData {
    @NotNull
    @JsonProperty("unitID")
    private UUID unitID;
    @NotNull
    @JsonProperty("assotiatedCardAlias")
    private String assotiatedCardAlias;
    @JsonProperty("maxHP")
    private int maxHP;
    @JsonProperty("currentHP")
    private int currentHP;
    @NotNull
    @JsonProperty("startPoint")
    private PointData startPoint;

    public UnitData(
            @NotNull UUID unitID,
            @NotNull String assotiatedCardAlias,
            int maxHP,
            int currentHP,
            @NotNull PointData startPoint){
        this.unitID = unitID;
        this.assotiatedCardAlias = assotiatedCardAlias;
        this.maxHP = maxHP;
        this.currentHP = currentHP;
        this.startPoint = startPoint;
    }

    @NotNull
    public UUID getUnitID(){
        return unitID;
    }

    public void setUnitID(@NotNull UUID unitID){
        this.unitID = unitID;
    }

    @NotNull
    public String getAssotiatedCardAlias(){
        return assotiatedCardAlias;
    }

    public void setAssotiatedCardAlias(@NotNull String assotiatedCardAlias){
        this.assotiatedCardAlias = assotiatedCardAlias;
    }

    public int getMaxHP(){
        return maxHP;
    }

    public void setMaxHP(int maxHP){
        this.maxHP = maxHP;
    }

    public int getCurrentHP(){
        return currentHP;
    }

    public void setCurrentHP(int currentHP){
        this.maxHP = currentHP;
    }

    @NotNull
    public PointData getStartPoint(){
        return startPoint;
    }

    public void setStartPoint(@NotNull PointData startPoint){
        this.startPoint = startPoint;
    }
}
