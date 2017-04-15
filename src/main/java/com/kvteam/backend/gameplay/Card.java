package com.kvteam.backend.gameplay;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by maxim on 03.04.17.
 */
public class Card {
    @NotNull
    private String alias;
    @NotNull
    private Side side;
    private int maxHP;
    private double attack;
    private double range;
    @Nullable
    private Point startPosition;

    // Таким способом можно создавать только из json-файла.
    // Для Jackson доступно будет через рефлексию
    @JsonCreator
    private Card(@JsonProperty("alias") @NotNull String alias,
         @JsonProperty("side") @NotNull Side side,
         @JsonProperty("maxHP") int maxHP,
         @JsonProperty("attack") double attack,
         @JsonProperty("range") double range){
        this.alias = alias;
        this.side = side;
        this.maxHP = maxHP;
        this.attack = attack;
        this.range = range;
        this.startPosition = null;
    }

    Card(@NotNull Card card){
        this.alias = card.alias;
        this.side = card.side;
        this.maxHP = card.maxHP;
        this.attack = card.attack;
        this.range = card.range;
        this.startPosition = card.startPosition;
    }

    Card(@NotNull Card card, @NotNull Point startPosition){
        this.alias = card.alias;
        this.side = card.side;
        this.maxHP = card.maxHP;
        this.attack = card.attack;
        this.range = card.range;
        this.startPosition = startPosition;
    }

    @NotNull
    public String getAlias(){
        return alias;
    }

    @NotNull
    public Side getSide(){
        return side;
    }

    public int getMaxHP(){
        return maxHP;
    }

    public double getAttack(){
        return attack;
    }

    public double getRange(){
        return range;
    }

    void setStartPosition(@Nullable Point position){
        this.startPosition = position;
    }

    @Nullable
    public Point getStartPosition(){
        return startPosition;
    }

    @Override
    public boolean equals(Object other){
        return hashCode() == other.hashCode();
    }

    @Override
    public int hashCode(){
        return alias.hashCode();
    }
}
