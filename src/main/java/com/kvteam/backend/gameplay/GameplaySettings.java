package com.kvteam.backend.gameplay;

import com.kvteam.backend.resources.RawResource;
import com.kvteam.backend.resources.Resource;
import com.kvteam.backend.resources.ResourceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by maxim on 04.04.17.
 */
@SuppressWarnings("SpringAutowiredFieldsWarningInspection")
@Component
@DependsOn("ResourceFactory")
public class GameplaySettings {
    private int maxMovesCount;
    private int maxCastleHP;
    @Autowired
    private ResourceFactory resourceFactory;


    @PostConstruct
    public void initSettings(){
        final RawResource res = resourceFactory.getRaw("data/game_settings.json");
        maxMovesCount = res.anyGet("maxMovesCount") != null ?
                        (int)res.anyGet("maxMovesCount") :
                        1;
        maxCastleHP = res.anyGet("maxCastleHP") != null ?
                      (int)res.anyGet("maxCastleHP") :
                      1;
    }

    public int getMaxMovesCount(){
        return maxMovesCount;
    }

    public int getMaxCastleHP(){
        return maxCastleHP;
    }
}
