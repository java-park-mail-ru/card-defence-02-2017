package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by maxim on 28.03.17.
 */
public class PointData {
    @JsonProperty("x")
    private int xCoord;
    @JsonProperty("y")
    private int yCoord;

    public PointData(){
        xCoord = yCoord = 0;
    }

    @JsonCreator
    public PointData(
            @JsonProperty("x") int x,
            @JsonProperty("y") int y){
        this.xCoord = x;
        this.yCoord = y;
    }

    public int getX(){
        return xCoord;
    }

    public int getY(){
        return yCoord;
    }

    public void setX(int x){
        this.xCoord = x;
    }

    public void setY(int y){
        this.yCoord = y;
    }
}
