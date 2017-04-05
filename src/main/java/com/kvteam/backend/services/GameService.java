package com.kvteam.backend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvteam.backend.dataformats.*;
import com.kvteam.backend.gameplay.*;
import com.kvteam.backend.websockets.IPlayerConnection;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Created by maxim on 26.03.17.
 */
@Service
public class GameService {
    private ObjectMapper mapper;
    private GameDbService dbService;
    private CardManager cardManager;
    private GameplaySettings gameplaySettings;

    public GameService(
            ObjectMapper mapper,
            GameDbService dbService,
            CardManager cardManager,
            GameplaySettings gameplaySettings){
        this.mapper = mapper;
        this.dbService = dbService;
        this.cardManager = cardManager;
        this.gameplaySettings = gameplaySettings;
    }

    private synchronized boolean setChosenCards(
            UUID gameID,
            Side side,
            String serialized){
        if(side == Side.ATTACKER) {
            dbService.setChosenAttackerCards(gameID, serialized);
        } else {
            dbService.setChosenDefenderCards(gameID, serialized);
        }
        return dbService.isBothReady(gameID);
    }


    private void startMatch(
            IPlayerConnection attacker,
            IPlayerConnection defender)
            throws SQLException, NullPointerException, IOException{
        if(attacker.getUsername() == null
                || defender.getUsername() == null){
            throw new NullPointerException();
        }
        final UUID gameID = dbService.insertNewMatch(
                attacker.getUsername(),
                defender.getUsername(),
                gameplaySettings.getMaxMovesCount(),
                gameplaySettings.getMaxCastleHP()
        );
        attacker.markAsPlaying(gameID);
        defender.markAsPlaying(gameID);

        final List<CardData> forAttack = cardManager
                .getCardsForMove(Side.ATTACKER, 0)
                .stream()
                .map(p -> new CardData(p.getAlias()))
                .collect(Collectors.toList());
        final List<CardData> forDefence = cardManager
                .getCardsForMove(Side.DEFENDER, 0)
                .stream()
                .map(p -> new CardData(p.getAlias()))
                .collect(Collectors.toList());
        dbService.insertMove(
                gameID,
                gameplaySettings.getMaxCastleHP(),
                mapper.writeValueAsString(forAttack),
                mapper.writeValueAsString(forDefence)
        );

        final GameStartData startDataAttack = new GameStartData(
                gameID,
                defender.getUsername(),
                "attack",
                gameplaySettings.getMaxMovesCount(),
                gameplaySettings.getMaxCastleHP(),
                forAttack);

        final GameStartData startDataDefence = new GameStartData(
                gameID,
                attacker.getUsername(),
                "defence",
                gameplaySettings.getMaxMovesCount(),
                gameplaySettings.getMaxCastleHP(),
                forDefence);
        attacker.send(mapper.writeValueAsString(startDataAttack));
        defender.send(mapper.writeValueAsString(startDataDefence));
    }

    private void processChatMessage(
            IPlayerConnection me,
            IPlayerConnection other,
            MessageClientData data) throws IOException {
        // Проверено выше
        //noinspection ConstantConditions
        final MessageServerData serverData = new MessageServerData(
                me.getGameID(),
                me.getUsername(),
                data.getMessage()
        );
        other.send(mapper.writeValueAsString(serverData));
    }

    private void processReady(
            IPlayerConnection me,
            IPlayerConnection other,
            Side side,
            ReadyClientData clientData) throws IOException{
        if(me.getGameID() == null){
            throw new NullPointerException();
        }
        final UUID gameID = me.getGameID();
        // Тут просто заполняем выбранные карты.

        final String serialized = mapper.writeValueAsString(clientData.getCards());
        // Синхронная вставка, узкое место.
        if(!setChosenCards(gameID, side, serialized)){
            return;
        }

        // А вот тут уже интересно. Считаем ход
        // Получаем разрешенные и выбранные карты и сопоставляем их
        final List<Card> availableCards = dbService.getAvailableCardsForCurrentMove(me.getGameID());
        final List<Card> chosenCards = dbService.getChosenCardsForCurrentMove(me.getGameID());
        final List<Card> cards = chosenCards.stream().filter(availableCards::contains).collect(Collectors.toList());
        // Предыдущий ход, если он был
        final Move previousMove = dbService.getLastMove(me.getGameID());
        // Составляем базовую информацию о ходе
        final Move move = previousMove != null ?
                                new Move(previousMove) : // Если это не первый ход, за основу предыдущий
                                new Move(gameplaySettings.getMaxCastleHP()); // Иначе первый инициализируем
        MoveProcessor.processMove(cards, move);

        // Посмотреть что в итоге получилось в move: продолжать игру или закончить
        // Собрать ответ
        // Записать в базу
        // Выделить еще карт, если нужно
        // Отправить ответ
        final int currentMove = move.getCurrentMove();
        final GameFinishingServerData data = new GameFinishingServerData(
                gameID,
                currentMove,
                move.getInitialCastleHP(),
                move.getCurrentCastleHP() <= 0,
                move.getUnits().stream().map(p ->
                        new UnitData(
                                p.getUnitID(),
                                p.getAssotiatedCardAlias(),
                                p.getMaxHP(),
                                p.getCurrentHP(),
                                new PointData(p.getStartPoint()))).collect(Collectors.toList()),
                move.getActions().stream().map(p ->
                        new ActionData(
                                p.getActor().getUnitID(),
                                p.getActionType().toString(),
                                p.getActionParams(),
                                p.getBeginOffset(),
                                p.getEndOffset()
                        )
                ).collect(Collectors.toList())
        );

        // Запись в базу информации о ходе(следующему пригодится)
        final List<UnitData> alive =  move.getAliveUnits().stream().map(p ->
                new UnitData(
                        p.getUnitID(),
                        p.getAssotiatedCardAlias(),
                        p.getMaxHP(),
                        p.getCurrentHP(),
                        new PointData(p.getStartPoint()))).collect(Collectors.toList());
        dbService.completeMove(
                gameID,
                data.getCurrentMove(),
                data.getCastleHP(),
                mapper.writeValueAsString(data.getUnits()),
                mapper.writeValueAsString(alive),
                mapper.writeValueAsString(data.getActions()));
        // Возвращаем клиенту все что насчитали
        final String answer = mapper.writeValueAsString(data);
        me.send(answer);
        other.send(answer);
    }

    private void processRenderComplete(
            IPlayerConnection me,
            IPlayerConnection other,
            RenderCompleteClientData data) throws IOException{
        try {
            if(me.getGameID() == null){
                throw new NullPointerException();
            }
            dbService.completeMatch(me.getGameID(), GameDbService.WinnerType.ATTACKER);
        }catch (SQLException | NullPointerException e){
            e.printStackTrace();
        }

        me.close();
        other.close();
    }

    private void processClose(
            IPlayerConnection me,
            IPlayerConnection other,
            CloseClientData data){
        try{
            me.close();
            other.close();
        } catch (IOException ignored) {

        }
    }

    private void receive(
            IPlayerConnection me,
            IPlayerConnection other,
            Side side,
            String message){
        if(me.getGameID() == null
                || me.getUsername() == null ){
            throw new NullPointerException();
        }
        try {
            final GameClientData baseData = mapper.readValue(message, GameClientData.class);
            if(baseData.getStatus().equals(GameClientData.SEND_CHAT_MESSAGE)){
                final MessageClientData data = mapper.readValue(message, MessageClientData.class);
                processChatMessage(me, other, data);
            } else if(baseData.getStatus().equals(GameClientData.READY)) {
                final ReadyClientData data = mapper.readValue(message, ReadyClientData.class);
                processReady(me, other, side, data);
            } else if(baseData.getStatus().equals(GameClientData.RENDER_COMPLETE)) {
                final RenderCompleteClientData data = mapper.readValue(message, RenderCompleteClientData.class);
                processRenderComplete(me, other, data);
            } else if(baseData.getStatus().equals(GameClientData.CLOSE)) {
                final CloseClientData data = mapper.readValue(message, CloseClientData.class);
                processClose(me, other, data);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close(IPlayerConnection me, IPlayerConnection other){
        try {
            other.close();
        } catch(IOException ignored) {

        }
    }

    public void startGame(IPlayerConnection attacker, IPlayerConnection defender){
        try {
            // При получении информации с клиента будет вызываться receive,
            // Причем, за счет замыканий, в методе будут доступны оба игрока
            attacker.onReceive((conn, str) -> receive(conn, defender, Side.ATTACKER, str));
            defender.onReceive((conn, str) -> receive(conn, attacker, Side.DEFENDER, str));
            attacker.onClose((conn, status) -> close(conn, defender));
            defender.onClose((conn, status) -> close(conn, attacker));

            startMatch(attacker, defender);
        } catch (IOException | NullPointerException | SQLException e) {
            attacker.markAsErrorable();
            defender.markAsErrorable();
            attacker.onReceive(null);
            attacker.onClose(null);
            defender.onReceive(null);
            defender.onReceive(null);
            try {
                attacker.close();
                defender.close();
            } catch (IOException ignored) {

            }
        }
    }

}
