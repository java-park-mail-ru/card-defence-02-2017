package com.kvteam.backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvteam.backend.dataformats.CardData;
import com.kvteam.backend.dataformats.PositionedCardData;
import com.kvteam.backend.dataformats.UnitData;
import com.kvteam.backend.gameplay.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by maxim on 02.04.17.
 */
@Service
public class GameDbService {
    private static final String SQL_INSERT_MATCH =
            "insert into matches(attacker, defender, max_moves, castlehp)\n" +
            "  select\n" +
            "    (select ID from users where username = :attacker limit 1),\n" +
            "    (select ID from users where username = :defender limit 1),\n" +
            "    :maxMoves,\n" +
            "    :castleMaxHP\n" +
            "returning ID";
    private static final String SQL_COMPLETE_MATCH =
            "update \n" +
            "  matches\n" +
            "set \n" +
            "  winner =\n" +
            "        case when :winner = 'attacker' then attacker\n" +
            "             else defender\n" +
            "        end,\n" +
            "  end_time = now()\n" +
            "where ID = :matchID;";

    private static final String SQL_INSERT_MOVE =
            "insert into moves(ID, move_number, initial_castle_hp, attacker_available_cards, defender_available_cards)\n" +
            "select \n" +
            "  :matchID,\n" +
            "  coalesce(\n" +
            "      (\n" +
            "        select \n" +
            "          move_number + 1 \n" +
            "        from moves m\n" +
            "        where m.ID = :matchID\n" +
            "        order by move_number desc\n" +
            "        limit 1 \n" +
            "      ), \n" +
            "      1\n" +
            "  ),\n" +
            "  :initialCastleHP,\n" +
            "  :attackerCards,\n" +
            "  :defenderCards\n" +
            "returning move_number;\n";

    private static final String SQL_SET_CHOSEN_CARDS_ATTACKER =
            "update\n" +
            "  moves\n" +
            "set\n" +
            "  attacker_chosen_cards = :cards\n" +
            "where\n" +
            "  rowid = (\n" +
            "    select \n" +
            "      rowid \n" +
            "    from moves \n" +
            "    where id = :id\n" +
            "    order by move_number desc\n" +
            "    limit 1\n" +
            "  );";

    private static final String SQL_SET_CHOSEN_CARDS_DEFENDER =
            "update\n" +
            "  moves\n" +
            "set\n" +
            "  defender_chosen_cards = :cards\n" +
            "where\n" +
            "  rowid = (\n" +
            "    select \n" +
            "      rowid \n" +
            "    from moves \n" +
            "    where id = :id\n" +
            "    order by move_number desc\n" +
            "    limit 1\n" +
            "  );";

    private static final String SQL_IS_BOTH_READY =
            "select\n" +
            "  cast(attacker_chosen_cards is not null\n" +
            "      and defender_chosen_cards is not null as boolean)\n" +
            "from moves\n" +
            "where id = :matchID\n" +
            "order by move_number desc\n" +
            "limit 1;";

    private static final String SQL_SET_ATTACKER_RENDER_COMPLETE =
            "update\n" +
            "  moves\n" +
            "set\n" +
            "  attacker_render_complete = True\n" +
            "where\n" +
            "  rowid = (\n" +
            "    select \n" +
            "      rowid \n" +
            "    from moves \n" +
            "    where id = :id\n" +
            "    order by move_number desc\n" +
            "    limit 1\n" +
            "  );";

    private static final String SQL_SET_DEFENDER_RENDER_COMPLETE =
            "update\n" +
            "  moves\n" +
            "set\n" +
            "  defender_render_complete = True\n" +
            "where\n" +
            "  rowid = (\n" +
            "    select \n" +
            "      rowid \n" +
            "    from moves \n" +
            "    where id = :id\n" +
            "    order by move_number desc\n" +
            "    limit 1\n" +
            "  );";


    private static final String SQL_IS_BOTH_RENDER_COMPLETE =
            "select\n" +
            "  case when attacker_render_complete is True\n" +
            "              and defender_render_complete is True\n" +
            "    then 1\n" +
            "    else 0\n" +
            "  end\n" +
            "from moves\n" +
            "where ID = :matchID\n" +
            "order by move_number desc\n" +
            "limit 1;";

    private static final String SQL_COMPLETE_MOVE =
            "update\n" +
            "  moves\n" +
            "set\n" +
            "  current_castle_hp = :currentCastleHP,\n" +
            "  units = :units,\n" +
            "  alive_units = :aliveUnits,\n" +
            "  actions = :actions\n" +
            "where ID = :matchID and move_number = :moveNumber;";

    private static final String SQL_IS_PLAYING_NOW =
            "select \n" +
            "  count(*) \n" +
            "from matches m\n" +
            "join users u on m.attacker = u.ID or m.defender = u.ID\n" +
            "where winner is null and u.username = :username";

    private static final String SQL_GET_LAST_MOVE =
            "select\n" +
            "  move_number,\n" +
            "  current_castle_hp,\n" +
            "  alive_units\n" +
            "from moves\n" +
            "where ID = :matchID and current_castle_hp is not null\n" +
            "order by move_number desc;";

    private static final String SQL_AVAILABLE_CARDS =
            "select\n" +
            "  attacker_available_cards,\n" +
            "  defender_available_cards\n" +
            "from moves\n" +
            "where ID = :matchID\n" +
            "order by move_number desc\n" +
            "limit 1;";

    private static final String SQL_CHOSEN_CARDS =
            "select\n" +
            "  attacker_chosen_cards,\n" +
            "  defender_chosen_cards\n" +
            "from moves\n" +
            "where ID = :matchID\n" +
            "order by move_number desc\n" +
            "limit 1;";


    enum WinnerType{
        ATTACKER,
        DEFENDER
    }

    private NamedParameterJdbcTemplate template;
    private ObjectMapper objectMapper;
    private CardManager cardManager;

    public GameDbService(
            NamedParameterJdbcTemplate template,
            ObjectMapper objectMapper,
            CardManager cardManager){
        this.template = template;
        this.objectMapper = objectMapper;
        this.cardManager = cardManager;
    }


    public UUID insertNewMatch(
            @NotNull String attacker,
            @NotNull String defender,
            int maxMoves,
            int castleMaxHP)
                throws SQLException{
        final Map<String, Object> params = new HashMap<>();
        params.put("attacker", attacker);
        params.put("defender", defender);
        params.put("maxMoves", maxMoves);
        params.put("castleMaxHP", castleMaxHP);

        return template.queryForObject(
                SQL_INSERT_MATCH,
                params,
                UUID.class
        );
    }

    public void completeMatch(
            @NotNull UUID matchID,
            @NotNull WinnerType winner)
                throws SQLException{
        final Map<String, Object> params = new HashMap<>();
        params.put("winner",  winner == WinnerType.ATTACKER ? "attacker" : "defender");
        params.put("matchID", matchID);
        template.update(
                SQL_COMPLETE_MATCH,
                params
        );
    }

    /**
     * Первичная вставка записи о ходе
     * На данном этапе указываются только доступные для выбора карты
     * @param matchID - иденификатор матча
     * @param attackerAvailableCards - сериализованные из JSON в String данные CardData
     * @param defenderAvailableCards - сериализованные из JSON в String данные CardData
     * @return возвращает номер хода
     */
    public int insertMove(
        @NotNull UUID matchID,
        int castleHP,
        @NotNull String attackerAvailableCards,
        @NotNull String defenderAvailableCards){
        final Map<String, Object> params = new HashMap<>();
        params.put("matchID", matchID);
        params.put("attackerCards", attackerAvailableCards);
        params.put("defenderCards", defenderAvailableCards);
        params.put("initialCastleHP", castleHP);
        return template.queryForObject(
                SQL_INSERT_MOVE,
                params,
                Integer.class
        );
    }

    public void setChosenAttackerCards(
            @NotNull UUID matchID,
            @NotNull String cards){
        final Map<String, Object> params = new HashMap<>();
        params.put("id", matchID);
        params.put("cards", cards);
        template.update(
                SQL_SET_CHOSEN_CARDS_ATTACKER,
                params
        );
    }

    public void setChosenDefenderCards(
            @NotNull UUID matchID,
            @NotNull String cards){
        final Map<String, Object> params = new HashMap<>();
        params.put("id", matchID);
        params.put("cards", cards);
        template.update(
                SQL_SET_CHOSEN_CARDS_DEFENDER,
                params
        );
    }

    public boolean isBothReady(@NotNull UUID matchID){
        final Map<String, Object> params = new HashMap<>();
        params.put("matchID", matchID);
        return template.queryForObject(
                SQL_IS_BOTH_READY,
                params,
                Boolean.class
        );
    }

    public void setDefenderRenderComplete(
            @NotNull UUID matchID){
        final Map<String, Object> params = new HashMap<>();
        params.put("id", matchID);
        template.update(
                SQL_SET_DEFENDER_RENDER_COMPLETE,
                params
        );
    }

    public void setAttackerRenderComplete(
            @NotNull UUID matchID){
        final Map<String, Object> params = new HashMap<>();
        params.put("id", matchID);
        template.update(
                SQL_SET_ATTACKER_RENDER_COMPLETE,
                params
        );
    }

    public boolean isBothRenderComplete(@NotNull UUID matchID){
        final Map<String, Object> params = new HashMap<>();
        params.put("matchID", matchID);
        return template.queryForObject(
                SQL_IS_BOTH_RENDER_COMPLETE,
                params,
                Boolean.class
        );
    }

    public void completeMove(
            @NotNull UUID matchID,
            int  moveNumber,
            int  currentCastleHP,
            @NotNull String units,
            @NotNull String aliveUnits,
            @NotNull String actions){

        final Map<String, Object> params = new HashMap<>();
        params.put("matchID", matchID);
        params.put("moveNumber", moveNumber);
        params.put("currentCastleHP", currentCastleHP);
        params.put("units", units);
        params.put("aliveUnits", aliveUnits);
        params.put("actions", actions);
        template.update(
                SQL_COMPLETE_MOVE,
                params
        );
    }

    @Nullable
    public Move getLastMove(@NotNull UUID matchID){
        final Map<String, Object> params = new HashMap<>();
        params.put("matchID", matchID);
        final List<Object> readedValues = new ArrayList<>();

        template.query(
                SQL_GET_LAST_MOVE,
                params,
                ((resultSet, i) ->  {
                    if( i == 0 ){
                        readedValues.add(resultSet.getInt(1));
                        readedValues.add(resultSet.getInt(2));
                        readedValues.add(resultSet.getString(3));
                    }
                    return null;
                })
        );
        if(readedValues.isEmpty()){
            return null;
        }


        final int currentMove = Integer.parseInt(readedValues.get(0).toString());
        final int castleHP = Integer.parseInt(readedValues.get(1).toString());
        final List<Unit> units = new ArrayList<>();
        try {
            final List<UnitData> unitDataList =
                    Arrays.asList(
                            objectMapper.readValue(
                                    readedValues.get(2).toString(),
                                    UnitData[].class)
                    );
            for(UnitData unitData : unitDataList){
                final Card assotiatedCard =
                        cardManager.getCard(unitData.getAssotiatedCardAlias());
                if(assotiatedCard != null){
                    units.add(
                            new Unit(assotiatedCard,
                                    unitData.getUnitID(),
                                    unitData.getStartPoint().toPoint(),
                                    unitData.getCurrentHP())
                    );
                }
            }
        } catch (IOException e) {
            units.clear();
            e.printStackTrace();
        }

        return new Move(currentMove, castleHP, units);
    }

    public List<Card> getAvailableCardsForCurrentMove(@NotNull UUID matchID){
        final Function<String, Stream<CardData>> deserializeToStream =
                (source) -> {
                    try {
                        return Arrays.stream(objectMapper.readValue(source, CardData[].class));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return Stream.empty();
                };
        final Map<String, Object> params = new HashMap<>();
        params.put("matchID", matchID);
        final List<String> rawCards = new ArrayList<>();
        template.query(
                SQL_AVAILABLE_CARDS,
                params,
                (resultSet, i) ->
                        rawCards.addAll(
                                Arrays.asList(
                                        resultSet.getString(1),
                                        resultSet.getString(2)))
        );

        return rawCards
                .stream()
                .flatMap(deserializeToStream::apply)
                .map( p -> cardManager.getCard(p.getAlias()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Card> getChosenCardsForCurrentMove(@NotNull UUID matchID){
        final Function<String, Stream<PositionedCardData>> deserializeToStream =
                (source) -> {
                    try {
                        return Arrays.stream(objectMapper.readValue(source, PositionedCardData[].class));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return Stream.empty();
                };
        final Map<String, Object> params = new HashMap<>();
        params.put("matchID", matchID);
        final List<String> rawCards = new ArrayList<>();
        template.query(
                SQL_CHOSEN_CARDS,
                params,
                (resultSet, i) ->
                        rawCards.addAll(
                                Arrays.asList(
                                        resultSet.getString(1),
                                        resultSet.getString(2)))
        );

        return rawCards
                .stream()
                .flatMap(deserializeToStream::apply)
                .map( p -> cardManager.getCard(p.getAlias(), p.getPointData().toPoint()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

    }




    public boolean isPlayingNow(
            @NotNull String username){
        final Map<String, Object> params = new HashMap<>();
        params.put("username", username);
        return template.queryForObject(
                SQL_IS_PLAYING_NOW,
                params,
                Integer.class) > 0 ;
    }

}
