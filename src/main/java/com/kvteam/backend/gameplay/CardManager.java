package com.kvteam.backend.gameplay;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by maxim on 03.04.17.
 */
public class CardManager {
    private Random rand;
    // Потом должно заполняться из файла
    private Map<String, Card> cards;

    public CardManager(@Nullable Card[] cards){
        this.rand = new Random();
        this.cards = new HashMap<>();
        if(cards != null) {
            for (Card card : cards) {
                this.cards.put(card.getAlias(), card);
            }
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
