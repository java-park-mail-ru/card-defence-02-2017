package com.kvteam.backend.gameplay;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kvteam.backend.dataformats.PointData;

/**
 * Created by maxim on 04.04.17.
 */
public class Point {
    private int xCoord;
    private int yCoord;

    public Point(){
        xCoord = yCoord = 0;
    }

    public Point(
            int x,
            int y){
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
