package com.kvteam.backend.gameplay;

import com.kvteam.backend.exceptions.MoveProcessorException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by maxim on 03.04.17.
 */
public final class MoveProcessor {
    @SuppressWarnings("PublicField")
    private static class MoveProcessorContext{
        public List<Unit> attackUnits;
        public List<Unit> defenceUnits;
        public Map<Unit, Action> activeActions;
        public Move move;
        public Action topTowerAction;
        public Action bottomTowerAction;
        public double castleRange;
        public int castleAttack;
        public int castleTimeAttack;
        public int time;
    }

    private static final double DOUBLE_COMPARE_PRECISION = 0.000001;
    private static final int TIME_LIMIT = 60000;
    private static final int TIME_DELTA = 100;
    private static final int LINE_COUNT = 4;

    private MoveProcessor(){}

    public static void processMove(
            GameplaySettings settings,
            List<Card> chosenCards,
            Move move) throws MoveProcessorException{
        for(Card card: chosenCards){
            final Point pos = card.getStartPosition();
            if( pos != null
                    && 0 <= pos.getX() && pos.getX() < 3
                    && 0 <= pos.getY() && pos.getY() < 4){
                move.addUnit(new Unit(card, pos));
            }
        }

        final MoveProcessorContext context = new MoveProcessorContext();
        context.move = move;
        context.activeActions = new HashMap<>();
        context.topTowerAction = null;
        context.bottomTowerAction = null;
        context.castleRange = settings.getCastleRange();
        context.castleAttack = settings.getCastleAttack();
        context.castleTimeAttack = settings.getCastleTimeAttack();
        context.attackUnits = move.getAliveUnits()
                .stream()
                .filter( p -> p.getSide() == Side.ATTACKER)
                .collect(Collectors.toList());
        context.defenceUnits = move.getAliveUnits()
                .stream()
                .filter( p -> p.getSide() == Side.DEFENDER)
                .collect(Collectors.toList());
        context.time = 0;

        if(context.attackUnits.isEmpty()
                || context.defenceUnits.isEmpty()){
            throw new MoveProcessorException("Я не придумал еще что тут сделать");
        }

        while(isMoveContinuous(context)){
            removeOldActiveActions(context);

            // TODO: значения из настроек игры
            castleAttack(context);

            towerAttack(context);

            unitsAttack(context);

            unitsDie(context);

            moveUnits(context);

            context.time += TIME_DELTA;
        }
    }

    private static boolean isMoveContinuous(MoveProcessorContext context){
        return context.move.getCurrentCastleHP() != 0
                && !context.attackUnits.isEmpty()
                && context.time < TIME_LIMIT;
    }

    private static void removeOldActiveActions(MoveProcessorContext context){
        context.activeActions
                .entrySet()
                .removeIf( p -> p.getValue().getEndOffset() < context.time
                        || p.getKey().getCurrentHP() <= 0);
        if(context.topTowerAction != null
                && context.topTowerAction.getEndOffset() < context.time ){
            context.topTowerAction = null;
        }
        if(context.bottomTowerAction != null
                && context.bottomTowerAction.getEndOffset() < context.time ){
            context.bottomTowerAction = null;
        }
    }

    private static void castleAttack(MoveProcessorContext context){
        // Выбираются все юниты в расстоянии атак башен
        // Определяется какая башня может по ним попасть(верхняя или нижняя)
        // Если эта башня в текущий момент не занята, то юнит атакуется
        context.attackUnits
                .stream()
                .filter( unit -> doubleGreaterOrEqual(context.castleRange, 1 - unit.getPositionOffset()))
                .forEach( unit -> chooseCastleTowerAndAttackUnit(context, unit));
    }

    private static void unitsAttack(MoveProcessorContext context){
        // В активных действиях юнит может или идти или атаковать
        // Если юнит дошел до того, что может атаковать, ему следует переключиться
        // с ходьбы на атаку
        for(Unit unit: context.attackUnits){
            final Unit nearestTower = getNearestTower(context, unit.getStartPoint().getY());
            final Action currentAction = context.activeActions.get(unit);
            if(nearestTower != null
                    && doubleGreaterOrEqual(unit.getPositionOffset() + unit.getRange(), startPointXToOffset(nearestTower))
                    && (currentAction == null || currentAction.getActionType().equals(ActionType.MOVE))){
                unitAttackTower(context, unit, nearestTower);
            }else if( nearestTower == null
                    && unit.getPositionOffset() + unit.getRange() >= 1
                    && (currentAction == null || currentAction.getActionType().equals(ActionType.MOVE))){
                unitAttackCastle(context, unit);
            }

        }
    }

    private static void towerAttack(MoveProcessorContext context){
        // Выбираем башни с переднего плана
        // Выбираем ближайшего юнита, которого может атаковать башня
        // Если звезды сходятся, то добавляем экншены
        for(int line = 0; line < LINE_COUNT; line++){
            final Unit nearestTower = getNearestTower(context, line);
            final Unit nearestUnit = getNearestUnit(context, line);
            if(nearestTower != null
                    && nearestUnit != null
                    && context.activeActions.get(nearestTower) == null
                    && doubleGreaterOrEqual(nearestUnit.getPositionOffset(), startPointXToOffset(nearestTower) - nearestTower.getRange())) {
                towerAttackUnit(context, nearestTower, nearestUnit);
            }
        }
    }

    private static void unitsDie(MoveProcessorContext context){
        context.defenceUnits.forEach(u -> dieIfNeeded(context, u));
        context.defenceUnits.removeIf(tower -> tower.getCurrentHP() <= 0);
        context.attackUnits.forEach(u -> dieIfNeeded(context, u));
        context.attackUnits.removeIf(unit -> unit.getCurrentHP() <= 0);
        context.move.getAliveUnits().removeIf(unit -> unit.getCurrentHP() <= 0);
    }

    private static void moveUnits(MoveProcessorContext context){
        // Если юнит ничем не занят и не уперся в башню или в замок,
        // то в активные действия ему прибавляем ходьбу
        context.attackUnits
                .stream()
                .filter( p -> context.activeActions.get(p) == null
                        && !restAgainstObstacle(p, context))
                .forEach( unit -> addMoveAction(context, unit));
        // Двигаем юнитов, у которых в активных действиях ходьба
        context.attackUnits
                .stream()
                .filter(p -> context.activeActions.containsKey(p)
                        && context.activeActions.get(p).getActionType().equals(ActionType.MOVE))
                .forEach(unit -> moveEachUnit(context, unit));
    }






    private static boolean doubleGreaterOrEqual(double d1, double d2){
        return d1 > d2 || Math.abs(d1 - d2) < DOUBLE_COMPARE_PRECISION;
    }

    /**
     * Просчет перемещения юнита с учетом его скорости(перемещение за секунду)
     * за время time в мс
     */
    private static double moveDistance(Unit unit, int time){
        //noinspection MagicNumber
        return unit.getVelocity() * (double)time / 1000.0;
    }

    /**
     * Расстояние до ближайшего препятствия (башня/замок)
     */
    private static double obstacleInFront(Unit unit, MoveProcessorContext context){
        final Unit nearestTower = getNearestTower(context, unit.getStartPoint().getY());
        return (nearestTower != null ? startPointXToOffset(nearestTower) : 1) - unit.getPositionOffset();
    }

    /**
     * Уперся ли атакующий юнит в башню или замок
     */
    private static boolean restAgainstObstacle(Unit unit, MoveProcessorContext context){
        // Корректно как для ~0, так и для отрицательных
        return obstacleInFront(unit, context) < DOUBLE_COMPARE_PRECISION;
    }

    /**
     * Уперся ли атакующий юнит в башню или замок
     */
    private static boolean restAgainstObstacle(double distance){
        // Корректно как для ~0, так и для отрицательных
        return distance < DOUBLE_COMPARE_PRECISION;
    }

    @SuppressWarnings("MagicNumber")
    private static double startPointXToOffset(Unit tower){
        final int x = tower.getStartPoint().getX();
        if(x == 2){
            // 18/20
            return 0.9;
        } else if(x == 1) {
            // 16/20
            return 0.8;
        } else {
            // некорректные данные
            return 1;
        }

    }

    private static Unit getNearestTower(MoveProcessorContext context, int line){
        return context.defenceUnits
                .stream()
                .filter(tower -> tower.getStartPoint().getY() == line)
                .sorted(Comparator.comparingInt(tower -> tower.getStartPoint().getX()))
                .findFirst()
                .orElse(null);
    }

    private static Unit getNearestUnit(MoveProcessorContext context, int line){
        return context.attackUnits
                .stream()
                .filter(unit -> unit.getStartPoint().getY() == line)
                .sorted(Comparator.comparingDouble(Unit::getPositionOffset).reversed())
                .findFirst()
                .orElse(null);
    }

    private static void chooseCastleTowerAndAttackUnit(
            MoveProcessorContext context,
            Unit unit){
        if(context.topTowerAction == null
                && (unit.getStartPoint().getY() == 0
                || unit.getStartPoint().getY() == 1)){
            castleTowerAttackUnit(
                    context,
                    "top",
                    unit);
        } else if (context.bottomTowerAction == null
                && (unit.getStartPoint().getY() == 2
                || unit.getStartPoint().getY() == 3)) {
            castleTowerAttackUnit(
                    context,
                    "bottom",
                    unit);
        }
    }

    private static void castleTowerAttackUnit(
            MoveProcessorContext context,
            String towerPosition,
            Unit unit){
        unit.decrementHP(context.castleAttack);
        final Action action = new Action(
                towerPosition.equals("top") ? CastleTowers.TOP: CastleTowers.BOTTOM,
                ActionType.CASTLE_ATTACK);
        action.addActionParam("tower", towerPosition);
        action.addActionParam("victim", unit.getUnitID());
        action.setBeginOffset(context.time);
        action.setEndOffset(context.time + context.castleTimeAttack);
        if(towerPosition.equals("top")){
            context.topTowerAction = action;
        }else{
            context.bottomTowerAction = action;
        }
        context.move.addAction(action);
        final Action getDamageAction = new Action(unit, ActionType.GET_DAMAGE);
        getDamageAction.addActionParam("damage", context.castleAttack);
        getDamageAction.setBeginOffset(context.time);
        getDamageAction.setEndOffset(context.time);
        context.move.addAction(getDamageAction);
    }

    private static void unitAttackTower(MoveProcessorContext context, Unit unit, Unit tower){
        final Action attack = new Action(unit, ActionType.ATTACK);
        attack.setBeginOffset(context.time);
        attack.setEndOffset(context.time + unit.getTimeAttack());
        attack.addActionParam("victim", tower.getUnitID());
        context.activeActions.put(unit, attack);
        context.move.addAction(attack);
        tower.decrementHP(unit.getAttack());
        final Action getDamageAction = new Action(tower, ActionType.GET_DAMAGE);
        getDamageAction.addActionParam("damage", unit.getAttack());
        getDamageAction.setBeginOffset(context.time);
        getDamageAction.setEndOffset(context.time);
        context.move.addAction(getDamageAction);
    }

    private static void unitAttackCastle(MoveProcessorContext context, Unit unit){
        final Action attack = new Action(unit, ActionType.ATTACK);
        attack.setBeginOffset(context.time);
        attack.setEndOffset(context.time + unit.getTimeAttack());
        context.activeActions.put(unit, attack);
        context.move.addAction(attack);
        context.move.decrementCastleHP(unit.getAttack());
    }

    private static void towerAttackUnit(MoveProcessorContext context, Unit tower, Unit unit){
        unit.decrementHP(tower.getAttack());
        final Action attack = new Action(tower, ActionType.ATTACK);
        attack.addActionParam("victim", unit.getUnitID());
        attack.setBeginOffset(context.time);
        attack.setEndOffset(context.time + tower.getTimeAttack());
        context.activeActions.put(tower, attack);
        context.move.addAction(attack);
        final Action getDamageAction = new Action(unit, ActionType.GET_DAMAGE);
        getDamageAction.addActionParam("damage", tower.getAttack());
        getDamageAction.setBeginOffset(context.time);
        getDamageAction.setEndOffset(context.time);
        context.move.addAction(getDamageAction);
    }

    private static void dieIfNeeded(MoveProcessorContext context, Unit unit){
        if(unit.getCurrentHP() <= 0){
            final Action action = context.activeActions.remove(unit);
            if(action != null){
                action.setEndOffset(context.time);
            }
            context.move.addAction(
                    new Action(unit, ActionType.DIE, new HashMap<>(), context.time, context.time)
            );
        }
    }

    private static void addMoveAction(MoveProcessorContext context, Unit unit){
        final Action action = new Action(unit, ActionType.MOVE);
        action.setBeginOffset(context.time);
        action.setEndOffset(context.time + TIME_DELTA);
        context.activeActions.put(unit, action);
        context.move.addAction(action);
    }

    private static void moveEachUnit(MoveProcessorContext context, Unit unit){
        final double distanceToObstacle = obstacleInFront(unit, context);
        final double distancePerTick = moveDistance(unit, TIME_DELTA);
        if(!restAgainstObstacle(distanceToObstacle)) {
            final double distance = Math.min(distancePerTick, distanceToObstacle);
            unit.incrementOffset(distance);
            context.activeActions.get(unit).setEndOffset(context.time + (int)(distance / distancePerTick * TIME_DELTA));
            context.activeActions.get(unit).addActionParam("pos", distance);
        }
    }

}
