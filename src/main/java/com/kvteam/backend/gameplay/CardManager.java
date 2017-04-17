package com.kvteam.backend.gameplay;

import com.kvteam.backend.resources.ResourceFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by maxim on 03.04.17.
 */
@SuppressWarnings("SpringAutowiredFieldsWarningInspection")
@Component
@DependsOn("ResourceFactory")
public class CardManager {
    private Random rand;
    // Потом должно заполняться из файла
    private Map<String, Card> cards;
    @Autowired
    private ResourceFactory resourceFactory;

    public CardManager(){
        this.rand = new Random();
        this.cards = new HashMap<>();
    }

    @PostConstruct
    public void initCards(){
        final List<Card> readed =
                    resourceFactory.getFromDir("data/cards/", Card.class);
        for(Card card: readed){
            cards.put(card.getAlias(), card);
        }
    }

    public List<Card> getCardsForMove(
            @NotNull Side side,
            int moveCount){
        final List<Card> onlySelectedSide = cards
                                        .values()
                                        .stream()
                                        .filter( p -> p.getSide() == side)
                                        .collect(Collectors.toList());
        Collections.shuffle(onlySelectedSide);
        final int numberOfCardsForMove = 1;
        return onlySelectedSide
                .stream()
                .limit(numberOfCardsForMove)
                .collect(Collectors.toList());
    }

    @Nullable
    public Card getCard(@NotNull String alias){
        final Card template = cards.get(alias);
        return template != null ?
                new Card(template):
                null;
    }

    @Nullable
    public Card getCard(@NotNull String alias, @NotNull Point startPosition){
        final Card template = cards.get(alias);
        return template != null ?
                new Card(template, startPosition):
                null;
    }
}
