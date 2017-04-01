package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;


/**
 * Created by maxim on 01.04.17.
 */
public class PositionedCardData extends CardData {
    @NotNull
    @JsonProperty("pos")
    private PointData pointData;

    @JsonCreator
    public PositionedCardData(
            @JsonProperty("alias") @NotNull String alias,
            @JsonProperty("pos") @NotNull PointData pointData) {
        super(alias);
        this.pointData = pointData;
    }

    @NotNull
    public PointData getPointData(){
        return pointData;
    }
}
