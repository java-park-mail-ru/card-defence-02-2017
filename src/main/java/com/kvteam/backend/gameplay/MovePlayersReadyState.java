package com.kvteam.backend.gameplay;

/**
 * Created by maxim on 23.04.17.
 */
public class MovePlayersReadyState {
    private boolean attackReady;
    private boolean defenceReady;
    private boolean attackRenderComplete;
    private boolean defenceRenderComplete;

    public MovePlayersReadyState(){
        attackReady = false;
        defenceReady = false;
        attackRenderComplete = false;
        defenceRenderComplete = false;
    }

    public void setAttackReady(){
        attackReady = true;
    }

    public void setDefenceReady(){
        defenceReady = true;
    }

    public void setAttackRenderComplete(){
        attackRenderComplete = true;
    }

    public void setDefenceRenderComplete(){
        defenceRenderComplete = true;
    }

    public boolean isBothReady(){
        return attackReady && defenceReady;
    }

    public boolean isBothRenderComplete(){
        return attackRenderComplete && defenceRenderComplete;
    }
}
