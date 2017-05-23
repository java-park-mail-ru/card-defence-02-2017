package com.kvteam.backend.gameplay;

/**
 * Created by maxim on 04.04.17.
 */
public final class ActionType {
    private static final String MOVE_STR = "move";
    private static final String ATTACK_STR = "attack";
    private static final String GET_DAMAGE_STR = "getdamage";
    private static final String DIE_STR = "die";
    private static final String CASTLE_ATTACK_STR = "castleattack";

    private String type;

    public static final ActionType MOVE = new ActionType(MOVE_STR);
    public static final ActionType ATTACK = new ActionType(ATTACK_STR);
    public static final ActionType GET_DAMAGE = new ActionType(GET_DAMAGE_STR);
    public static final ActionType DIE = new ActionType(DIE_STR);
    public static final ActionType CASTLE_ATTACK = new ActionType(CASTLE_ATTACK_STR);

    private ActionType(String type){
        this.type = type;
    }

    @Override
    public String toString(){
        return type;
    }

    @Override
    public boolean equals(Object other){
        return other instanceof ActionType && this.hashCode() == other.hashCode();
    }

    @Override
    public int hashCode(){
        return type.hashCode();
    }
}
