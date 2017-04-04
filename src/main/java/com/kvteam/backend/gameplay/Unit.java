package com.kvteam.backend.gameplay;

import com.kvteam.backend.dataformats.PointData;
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
    private double attack;
    private double range;
    @NotNull
    private Point startPoint;
    private double positionOffset;

    /**
     * Создание нового юнита
     * @param card
     */
    Unit(@NotNull Card card, @NotNull Point startPoint){
        unitID = UUID.randomUUID();
        assotiatedCardAlias = card.getAlias();
        side = card.getSide();
        maxHP = currentHP = card.getMaxHP();
        attack = card.getAttack();
        range = card.getRange();
        this.startPoint = startPoint;
        positionOffset = 0;
    }

    /**
     * Создание выжившего с предыдущего хода
     * @param card
     * @param unitID
     * @param startPoint
     * @param currentHP
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
        range = card.getRange();
        this.startPoint = startPoint;
        this.currentHP = currentHP;
        positionOffset = 0;
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
        currentHP -= currentHP - value > 0 ?
                        currentHP - value :
                        0;
        return currentHP;
    }

    public void die(){
        currentHP = 0;
    }

    public double getAttack(){
        return attack;
    }

    public double getRange(){
        return range;
    }

    @NotNull
    public Point getStartPoint(){
        return startPoint;
    }

    public double getPositionOffset(){
        return positionOffset;
    }

    public void incrementOffset(double offset){
        positionOffset += positionOffset + offset <= 1 ?
                            positionOffset + offset :
                            1;
    }
}
