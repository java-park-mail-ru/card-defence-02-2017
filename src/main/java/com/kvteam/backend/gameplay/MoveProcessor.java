package com.kvteam.backend.gameplay;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by maxim on 03.04.17.
 */
public final class MoveProcessor {
    private MoveProcessor(){}

    public static void processMove(
            @NotNull List<Card> chosenCards,
            @NotNull Move move){
        for(Card card: chosenCards){
            final Point pos = card.getStartPosition();
            if( pos != null
                    && 0 <= pos.getX() && pos.getX() < 3
                    && 0 <= pos.getY() && pos.getY() < 4){
                move.addUnit(new Unit(card, pos));
            }
        }
        final List<Unit> attackUnits = move.getAliveUnits()
                .stream()
                .filter( p -> p.getSide() == Side.ATTACKER)
                .collect(Collectors.toList());
        final List<Unit> defenceUnits = move.getAliveUnits()
                .stream()
                .filter( p -> p.getSide() == Side.DEFENDER)
                .collect(Collectors.toList());

        if(attackUnits.isEmpty()
                || defenceUnits.isEmpty()){
            throw new RuntimeException("Я не придумал еще что тут сделать");
        }

        // Здесь сейчас будет самая базовая логика:
        // 1. Два юнита на одной линии:
        //      Атакующий дошел до башни
        //      Башня атаковала
        //      Атакующий умер
        // 2. Юниты на разных линиях
        //      Атакующий дошел до замка
        //      Ударил замок

        final Unit attackUnit = attackUnits.get(0);
        final Unit defenceUnit = defenceUnits.get(0);
        if(attackUnit.getStartPoint().getY() == defenceUnit.getStartPoint().getY()){
            final Action moveAction = new Action(attackUnit, ActionType.MOVE);
            moveAction.setBeginOffset(50);
            moveAction.setEndOffset(3000);
            if(defenceUnit.getStartPoint().getX() == 1){
                moveAction.addActionParam("distance", 0.8);
                attackUnit.incrementOffset(0.8);
            } else {
                moveAction.addActionParam("distance", 0.9);
                attackUnit.incrementOffset(0.9);
            }
            move.addAction( moveAction );

            final Action attackAction = new Action(defenceUnit, ActionType.ATTACK);
            attackAction.setBeginOffset(3000);
            attackAction.setEndOffset(3200);
            move.addAction( attackAction );

            final Action dieAction = new Action(attackUnit, ActionType.DIE);
            dieAction.setBeginOffset(3200);
            dieAction.setEndOffset(3400);
            move.addAction( dieAction );
            attackUnit.die();
            // TODO: проверить, работает ли так
            move.getAliveUnits().remove(attackUnit);
        } else {
            final Action moveAction = new Action(attackUnit, ActionType.MOVE);
            moveAction.setBeginOffset(50);
            moveAction.setEndOffset(3500);
            moveAction.addActionParam("distance", 1);
            attackUnit.incrementOffset(1);
            move.addAction( moveAction );

            final Action attackAction = new Action(attackUnit, ActionType.ATTACK);
            attackAction.setBeginOffset(3500);
            attackAction.setEndOffset(3700);
            move.addAction( attackAction );
            move.decrementCastleHP(1);
        }
    }
}
