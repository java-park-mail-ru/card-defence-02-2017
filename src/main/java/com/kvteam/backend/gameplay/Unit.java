package com.kvteam.backend.gameplay;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Created by maxim on 03.04.17.
 */
public class Unit {
    @NotNull
    private UUID unitID;
    @NotNull
    private String assotiatedCardAlias;
    @NotNull
    private Side side;
    private int maxHP;
    private int currentHP;
    private int attack;
    private int timeAttack;
    private double range;
    private double velocity;
    @NotNull
    private Point startPoint;
    private double positionOffset;

    /**
     * Фиктивный юнит(например, башенка замка)
     */
    Unit(@NotNull UUID id, @NotNull String fictiveAlias){
        unitID = id;
        assotiatedCardAlias = fictiveAlias;
        side = Side.UNKNOWN;
        maxHP = currentHP = -1;
        timeAttack = -1;
        attack = -1;
        range = -1;
        velocity = -1;
        this.startPoint = new Point(-1, -1);
        positionOffset = 0;
    }

    /**
     * Создание нового юнита
     */
    Unit(@NotNull Card card, @NotNull Point startPoint){
        unitID = UUID.randomUUID();
        assotiatedCardAlias = card.getAlias();
        side = card.getSide();
        maxHP = currentHP = card.getMaxHP();
        attack = card.getAttack();
        timeAttack = card.getTimeAttack();
        range = card.getRange();
        velocity = card.getVelocity();
        this.startPoint = startPoint;
        positionOffset = 0;
    }

    /**
     * Создание выжившего с предыдущего хода
     */
    public Unit(
            @NotNull Card card,
            @NotNull UUID unitID,
            @NotNull Point startPoint,
            int currentHP){
        this.unitID = unitID;
        assotiatedCardAlias = card.getAlias();
        side = card.getSide();
        maxHP = card.getMaxHP();
        attack = card.getAttack();
        timeAttack = card.getTimeAttack();
        range = card.getRange();
        velocity = card.getVelocity();
        this.startPoint = startPoint;
        this.currentHP = currentHP;
        positionOffset = 0;
    }

    public Unit(@NotNull Unit unit){
        this.unitID = unit.unitID;
        this.assotiatedCardAlias = unit.assotiatedCardAlias;
        this.side = unit.side;
        this.maxHP = unit.maxHP;
        this.attack = unit.attack;
        this.timeAttack = unit.timeAttack;
        this.range = unit.range;
        this.velocity = unit.velocity;
        this.startPoint = unit.startPoint;
        this.currentHP = unit.currentHP;
        this.positionOffset = unit.positionOffset;
    }

    @NotNull
    public UUID getUnitID(){
        return unitID;
    }

    @NotNull
    public String getAssotiatedCardAlias(){
        return assotiatedCardAlias;
    }

    @NotNull
    public Side getSide(){
        return side;
    }

    public int getMaxHP(){
        return maxHP;
    }

    public int getCurrentHP(){
        return currentHP;
    }

    public int decrementHP(int value){
        currentHP = (currentHP - value > 0) ?
                    currentHP - value :
                    0;
        return currentHP;
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

    public double getVelocity(){
        return velocity;
    }

    @NotNull
    public Point getStartPoint(){
        return startPoint;
    }

    public double getPositionOffset(){
        return positionOffset;
    }

    public void incrementOffset(double offset){
        positionOffset = (positionOffset + offset <= 1) ?
                positionOffset + offset:
                1;
    }

    @Override
    public boolean equals(Object other){
        return other instanceof Unit && hashCode() == other.hashCode();
    }

    @Override
    public int hashCode(){
        return unitID.hashCode();
    }

    @Override
    public String toString(){
        return assotiatedCardAlias
                + '(' + side + ')'
                + " pos:" + startPoint.toString()
                + " offset:" + Double.toString(positionOffset)
                + " hp:" + Integer.toString(currentHP);
    }
}
