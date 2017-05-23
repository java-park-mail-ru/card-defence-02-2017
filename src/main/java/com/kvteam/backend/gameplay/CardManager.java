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
    private Map<UUID, List<Card>> pools;
    @Autowired
    private ResourceFactory resourceFactory;

    public CardManager(){
        this.rand = new Random();
        this.cards = new HashMap<>();
        this.pools = new HashMap<>();
    }

    @PostConstruct
    public void initCards(){
        final String[] names = resourceFactory.getJsonStringArray("data/cards/cards.json");
        for(String name: names){
            final Card card =
                    resourceFactory.get("data/cards/" + name, Card.class);
            cards.put(card.getAlias(), card);
        }
    }

    public void initPool(@NotNull UUID gameID) {
        pools.put(gameID, cards.values().stream().collect(Collectors.toList()));
    }

    public void deletePool(@NotNull UUID gameID) {
        pools.remove(gameID);
    }

    public List<Card> getCardsForMove(
            @NotNull UUID gameID,
            @NotNull Side side,
            int moveCount){
        final List<Card> onlySelectedSide = pools.get(gameID)
                                        .stream()
                                        .filter( p -> p.getSide() == side)
                                        .collect(Collectors.toList());
        Collections.shuffle(onlySelectedSide);
        final int numberOfCardsForMove = Math.min(moveCount, 4);
        final List<Card> cardsForMove = onlySelectedSide
                .stream()
                .limit(numberOfCardsForMove)
                .collect(Collectors.toList());
        pools.get(gameID).removeAll(cardsForMove);
        return cardsForMove;
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
