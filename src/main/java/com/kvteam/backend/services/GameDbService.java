package com.kvteam.backend.services;

import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;

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
            "             when :winner = 'defender' then defender\n" +
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

    private static final String SQL_COMPLETE_MOVE =
            "update\n" +
            "  moves\n" +
            "set\n" +
            "  current_castle_hp = :currentCastleHP,\n" +
            "  units = :units,\n" +
            "  alive_units = :aliveUnits,\n" +
            "  actions = :actions\n" +
            "where ID = :matchID and move_number = :moveNumber;";

    private static final String SQL_GET_MATCH_PLAYERS =
            "select\n" +
            "   winner\n" +
            "from matches\n" +
            "where ID = :matchID;";

    private static final String SQL_UPDATE_RATING =
            "update users set rating = rating + 1 where id = :winner;";

    enum WinnerType{
        ATTACKER,
        DEFENDER,
        NONE
    }

    private NamedParameterJdbcTemplate template;

    public GameDbService(NamedParameterJdbcTemplate template){
        this.template = template;
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
        if (winner == WinnerType.ATTACKER) {
            params.put("winner", "attacker");
        } else if (winner == WinnerType.DEFENDER) {
            params.put("winner", "attacker");
        } else {
            params.put("winner", null);
        }
        params.put("matchID", matchID);
        template.update(
                SQL_COMPLETE_MATCH,
                params
        );
    }

    public void updateRating(@NotNull UUID matchID) {
        final Map<String, Object> params = new HashMap<>();
        params.put("matchID", matchID);
        final UUID winner = template.queryForObject(
                SQL_GET_MATCH_PLAYERS,
                params,
                UUID.class);

        if(winner != null){
            params.clear();
            params.put("winner", winner);
            template.update(
                    SQL_UPDATE_RATING,
                    params
            );
        }
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
}
