package com.kvteam.backend.gameplay;

/**
 * Created by maxim on 04.04.17.
 */
@SuppressWarnings("EqualsAndHashcode")
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

    @Override
    public boolean equals(Object p){
        return p instanceof Point
                && xCoord == ((Point)p).xCoord
                && yCoord == ((Point)p).yCoord;
    }

    @Override
    public String toString(){
        return '(' + Integer.toString(xCoord) + ',' + Integer.toString(yCoord) + ')';
    }
}
