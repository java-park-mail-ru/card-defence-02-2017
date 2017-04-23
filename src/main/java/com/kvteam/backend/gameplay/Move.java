package com.kvteam.backend.gameplay;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;

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
        currentMove = 1;
        initialCastleHP = currentCastleHP = maxCastleHP;
        units = new ArrayList<>();
        aliveUnits = new ArrayList<>();
        actions = new ArrayList<>();
    }

    // Построение на основе предыдущего хода
    public Move(Move prevMove){
        this.currentMove = prevMove.currentMove + 1;
        this.initialCastleHP = this.currentCastleHP = prevMove.currentCastleHP;
        this.units = prevMove.aliveUnits.stream().map(Unit::new).collect(Collectors.toList());
        this.aliveUnits = prevMove.aliveUnits.stream().map(Unit::new).collect(Collectors.toList());
        this.actions = new ArrayList<>();
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

    public int decrementCastleHP(int value){
        currentCastleHP = (currentCastleHP - value > 0) ?
                currentCastleHP - value :
                0;
        return currentCastleHP;
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
        units.removeIf( u -> u.getStartPoint().equals(unit.getStartPoint()));
        aliveUnits.removeIf( u -> u.getStartPoint().equals(unit.getStartPoint()));
        units.add(new Unit(unit));
        aliveUnits.add(unit);
    }

    public List<Action> getActions(){
        return actions;
    }

    public void addAction(@NotNull Action action){
        actions.add(action);
    }

    @Override
    public String toString(){
        return "MoveNumber:" + currentMove
                + ", castleHP(initial/current):" + initialCastleHP + '/' + currentCastleHP
                + ", unitsCount:" + units.size()
                + ", aliveUnitsCount:" + aliveUnits.size()
                + ", actionsCount:" + actions.size();
    }
}
