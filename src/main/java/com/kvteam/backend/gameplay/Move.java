package com.kvteam.backend.gameplay;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import java.util.List;

/**
 * Created by maxim on 03.04.17.
 */
public class Move {
    private int currentMove;
    // Состояние замка в начале хода
    private int initialCastleHP;
    // Состояние замка в конце хода
    private int currentCastleHP;
    // Юниты в начале хода
    private List<Unit> units;
    // Оставшиеся в живых юниты в конце хода
    private List<Unit> aliveUnits;
    // Действия, которые приводят состояние замка и юнитов из начального в конечное
    private List<Action> actions;

    // Первый ход
    public Move(int maxCastleHP){
        currentMove = 0;
        initialCastleHP = currentCastleHP = maxCastleHP;
        units = new ArrayList<>();
        aliveUnits = new ArrayList<>();
        actions = new ArrayList<>();
    }

    // Построение на основе предыдущего хода
    public Move(@NotNull Move prevMove){
        this.currentMove = prevMove.currentMove + 1;
        this.initialCastleHP = this.currentCastleHP = prevMove.currentCastleHP;
        this.units = new ArrayList<>(prevMove.aliveUnits);
        this.aliveUnits = new ArrayList<>(prevMove.aliveUnits);
    }

    // Предыдущий ход, пихать это в processMove не следует
    public Move(
            int currentMove,
            int currentCastleHP,
            List<Unit> units){
        this.currentMove = currentMove;
        this.currentCastleHP = currentCastleHP;
        this.aliveUnits = units;
        this.actions = new ArrayList<>();
    }

    public int getCurrentMove(){
        return currentMove;
    }

    public int getInitialCastleHP(){
        return initialCastleHP;
    }

    public int getCurrentCastleHP(){
        return currentCastleHP;
    }

    public void decrementCastleHP(int value){
        currentCastleHP -= value;
    }

    // Это нужно будет скинуть на клиент
    public List<Unit> getUnits(){
        return units;
    }

    // Над этим будет вестись обработка
    public List<Unit> getAliveUnits(){
        return aliveUnits;
    }

    public void addUnit(@NotNull Unit unit){
        // Кидаем в оба массива
        // в первом будут хранится изначальные данные
        // над вторым будет вестись обработка
        // Предполагается, что массив будет заполнен до начала обработки
        units.add(unit);
        aliveUnits.add(unit);
    }

    public List<Action> getActions(){
        return actions;
    }

    public void addAction(@NotNull Action action){
        actions.add(action);
    }
}
