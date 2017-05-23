package com.kvteam.backend.gameplay;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kvteam.backend.resources.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by maxim on 03.04.17.
 */
public class Card extends Resource{
    @NotNull
    private String alias;
    @NotNull
    private Side side;
    private int maxHP;
    private int attack;
    private int timeAttack;
    private double range;
    private double velocity;
    @Nullable
    private Point startPosition;


    @SuppressWarnings("unused")
    @JsonCreator
    private Card(
         @JsonProperty("type") @NotNull String type,
         @JsonProperty("alias") @NotNull String alias,
         @JsonProperty("side") @NotNull Side side,
         @JsonProperty("maxHP") int maxHP,
         @JsonProperty("attack") int attack,
         @JsonProperty("timeAttack") int timeAttack,
         @JsonProperty("range") double range,
         @JsonProperty("velocity") double velocity){
        super(type);
        this.alias = alias;
        this.side = side;
        this.maxHP = maxHP;
        this.attack = attack;
        this.timeAttack = timeAttack;
        this.range = range;
        this.velocity = velocity;
        this.startPosition = null;
    }

    Card(@NotNull Card card){
        this.alias = card.alias;
        this.side = card.side;
        this.maxHP = card.maxHP;
        this.attack = card.attack;
        this.timeAttack = card.timeAttack;
        this.range = card.range;
        this.velocity = card.velocity;
        this.startPosition = card.startPosition;
    }

    Card(@NotNull Card card, @NotNull Point startPosition){
        this.alias = card.alias;
        this.side = card.side;
        this.maxHP = card.maxHP;
        this.attack = card.attack;
        this.timeAttack = card.timeAttack;
        this.range = card.range;
        this.velocity = card.velocity;
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

    public int getAttack(){
        return attack;
    }

    public int getTimeAttack() {
        return timeAttack;
    }

    public double getRange(){
        return range;
    }

    public double getVelocity() {
        return velocity;
    }

    @SuppressWarnings("unused")
    void setStartPosition(@Nullable Point position){
        this.startPosition = position;
    }

    @Nullable
    public Point getStartPosition(){
        return startPosition;
    }

    @Override
    public boolean equals(Object other){
        return other instanceof Card && hashCode() == other.hashCode();
    }

    @Override
    public int hashCode(){
        return alias.hashCode();
    }

    @Override
    public String toString(){
        return alias;
    }
}
