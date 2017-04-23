package com.kvteam.backend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvteam.backend.dataformats.*;
import com.kvteam.backend.exceptions.InvalidPlayerConnectionStateException;
import com.kvteam.backend.exceptions.MoveProcessorException;
import com.kvteam.backend.gameplay.*;
import com.kvteam.backend.websockets.IPlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Created by maxim on 26.03.17.
 */
@SuppressWarnings("OverlyBroadThrowsClause")
@Service
public class GameService {
    private ObjectMapper mapper;
    private GameDbService dbService;
    private CardManager cardManager;
    private GameplaySettings gameplaySettings;

    private ExecutorService dbExecutorService;
    @Value("db_thread_count")
    private String dbThreadCountStr;

    private Map<UUID, ConcurrentLinkedDeque<Move>> gameMovesCache;
    private Map<UUID, MovePlayersReadyState> currentMoveStates;
    private Map<UUID, List<Card>> availableCardsCache;
    private Map<UUID, List<Card>> chosenCardsCache;

    public GameService(
            ObjectMapper mapper,
            GameDbService dbService,
            CardManager cardManager,
            GameplaySettings gameplaySettings){
        this.mapper = mapper;
        this.dbService = dbService;
        this.cardManager = cardManager;
        this.gameplaySettings = gameplaySettings;

        try{
            final int dbThreadCount = Integer.parseInt(dbThreadCountStr);
            if(dbThreadCount <= 0){
                dbExecutorService = Executors.newCachedThreadPool();
            } else if(dbThreadCount == 1){
                dbExecutorService = Executors.newSingleThreadExecutor();
            } else {
                dbExecutorService = Executors.newFixedThreadPool(dbThreadCount);
            }
        } catch(NumberFormatException e){
            dbExecutorService = Executors.newCachedThreadPool();
        }

        gameMovesCache = new ConcurrentHashMap<>();
        currentMoveStates = new ConcurrentHashMap<>();
        availableCardsCache = new ConcurrentHashMap<>();
        chosenCardsCache = new ConcurrentHashMap<>();
    }

    @PreDestroy
    private void joinThreads(){
        dbExecutorService.shutdown();
    }

    private synchronized boolean setChosenCards(
            UUID gameID,
            Side side,
            List<PositionedCardData> cards) throws JsonProcessingException{
        final String serialized = mapper.writeValueAsString(cards);
        if(side == Side.ATTACKER) {
            dbExecutorService.execute(() -> dbService.setChosenAttackerCards(gameID, serialized));
            currentMoveStates.get(gameID).setAttackReady();
        } else {
            dbExecutorService.execute(() -> dbService.setChosenDefenderCards(gameID, serialized));
            currentMoveStates.get(gameID).setDefenceReady();
        }
        final List<Card> forCache = cards
                .stream()
                .map(p -> cardManager.getCard(p.getAlias(), p.getPointData().toPoint()))
                .collect(Collectors.toList());
        if(chosenCardsCache.get(gameID) == null){
            chosenCardsCache.put(gameID, forCache);
        } else {
            chosenCardsCache.get(gameID).addAll(forCache);
        }
        return currentMoveStates.get(gameID).isBothReady();
    }

    private synchronized boolean setRenderComplete(
            UUID gameID,
            Side side){
        if(side == Side.ATTACKER) {
            dbExecutorService.execute(() -> dbService.setDefenderRenderComplete(gameID));
            currentMoveStates.get(gameID).setAttackRenderComplete();
        } else {
            dbExecutorService.execute(() -> dbService.setAttackerRenderComplete(gameID));
            currentMoveStates.get(gameID).setDefenceRenderComplete();
        }
        return currentMoveStates.get(gameID).isBothRenderComplete();
    }

    private String serializeMoveResult(
            @NotNull UUID gameID,
            @NotNull Move move,
            @NotNull List<UnitData> units,
            @NotNull List<ActionData> actions) throws JsonProcessingException{
        final MoveResultGameServerData result;
        if(move.getCurrentCastleHP() <= 0
                || move.getCurrentMove() >= gameplaySettings.getMaxMovesCount()){
            result = new GameFinishingServerData(
                    gameID,
                    move.getCurrentMove(),
                    move.getInitialCastleHP(),
                    move.getCurrentCastleHP() <= 0,
                    units,
                    actions
            );
        } else {
            result = new ContinousGameServerData(
                    gameID,
                    move.getCurrentMove(),
                    move.getInitialCastleHP(),
                    units,
                    actions
            );
        }
        return mapper.writeValueAsString(result);
    }

    private List<Card> getAvailableCards(UUID gameID){
        if(availableCardsCache.containsKey(gameID)
                && availableCardsCache.get(gameID) != null){
            return availableCardsCache.get(gameID);
        }else{
            return dbService.getAvailableCardsForCurrentMove(gameID);
        }
    }

    private List<Card> getChosenCards(UUID gameID){
        if(chosenCardsCache.containsKey(gameID)
                && chosenCardsCache.get(gameID) != null){
            return chosenCardsCache.get(gameID);
        }else{
            return dbService.getChosenCardsForCurrentMove(gameID);
        }
    }

    @Nullable
    private Move getPreviousMove(UUID gameID){
        final Move move = gameMovesCache.containsKey(gameID) ?
                          gameMovesCache.get(gameID).poll() :
                          null;
        return move != null ? move : dbService.getLastMove(gameID) ;
    }

    @Nullable
    private Move getCurrentMove(UUID gameID){
        final Move move = gameMovesCache.containsKey(gameID) ?
                gameMovesCache.get(gameID).getLast() :
                null;
        return move != null ? move : dbService.getLastMove(gameID) ;
    }

    private void createMove(
            UUID gameID,
            List<Card> forAttack,
            List<Card> forDefence){
        currentMoveStates.put(gameID, new MovePlayersReadyState());
        // Запоминаются выданные карты в кэше
        final List<Card> availableCards = new ArrayList<>();
        availableCards.addAll(forAttack);
        availableCards.addAll(forDefence);
        availableCardsCache.put(gameID, availableCards);
        // Выданные карты сохраняются в базу
        final List<CardData> forDefenceCardData = forDefence
                .stream()
                .map(p -> new CardData(p.getAlias()))
                .collect(Collectors.toList());
        final List<CardData> forAttackCardData = forAttack
                .stream()
                .map(p -> new CardData(p.getAlias()))
                .collect(Collectors.toList());
        dbExecutorService.execute( () -> {
            try {
                dbService.insertMove(
                        gameID,
                        gameplaySettings.getMaxCastleHP(),
                        mapper.writeValueAsString(forAttackCardData),
                        mapper.writeValueAsString(forDefenceCardData)
                );
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }

    private void completeMove(
            UUID gameID,
            Move move,
            List<UnitData> units,
            List<UnitData> alive,
            List<ActionData> actions){
        dbExecutorService.execute( () -> {
            try {
                dbService.completeMove(
                        gameID,
                        move.getCurrentMove(),
                        move.getInitialCastleHP(),
                        mapper.writeValueAsString(units),
                        mapper.writeValueAsString(alive),
                        mapper.writeValueAsString(actions));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }

    private void completeMatch(
            UUID gameID,
            GameDbService.WinnerType winner,
            IPlayerConnection me,
            IPlayerConnection other) throws IOException{
        dbExecutorService.execute(() -> {
            try{
                dbService.completeMatch(gameID, winner);
            }catch(SQLException e){
                e.printStackTrace();
            }
        });
        currentMoveStates.remove(gameID);
        gameMovesCache.remove(gameID);
        me.markAsCompletion();
        other.markAsCompletion();
        me.close();
        other.close();

    }

    private void startMatch(
            IPlayerConnection attacker,
            IPlayerConnection defender)
            throws SQLException, NullPointerException, IOException{
        if(attacker.getUsername() == null
                || defender.getUsername() == null){
            throw new NullPointerException();
        }
        // Создается запись в базе о новом матче(синхронно)
        final UUID gameID = dbService.insertNewMatch(
                attacker.getUsername(),
                defender.getUsername(),
                gameplaySettings.getMaxMovesCount(),
                gameplaySettings.getMaxCastleHP()
        );
        // Создаем кэш для новой игры
        gameMovesCache.put(gameID, new ConcurrentLinkedDeque<>());

        // Выделяются карты для выбора
        final List<Card> forAttack = cardManager
                .getCardsForMove(Side.ATTACKER, 1);
        final List<Card> forDefence = cardManager
                .getCardsForMove(Side.DEFENDER, 1);

        // Ход записывается в кэши и в базу
        createMove(gameID, forAttack, forDefence);

        // Инфа переводится в формат общения клиент-сервер и отправляется и клиенту
        final List<CardData> forAttackCardData =
                forAttack.stream().map(p -> new CardData(p.getAlias())).collect(Collectors.toList());
        final List<CardData> forDefenceCardData =
                forDefence.stream().map(p -> new CardData(p.getAlias())).collect(Collectors.toList());
        final GameStartData startDataAttack = new GameStartData(
                gameID,
                defender.getUsername(),
                "attack",
                gameplaySettings.getMaxMovesCount(),
                gameplaySettings.getMaxCastleHP(),
                forAttackCardData);
        final GameStartData startDataDefence = new GameStartData(
                gameID,
                attacker.getUsername(),
                "defence",
                gameplaySettings.getMaxMovesCount(),
                gameplaySettings.getMaxCastleHP(),
                forDefenceCardData);
        attacker.send(mapper.writeValueAsString(startDataAttack));
        defender.send(mapper.writeValueAsString(startDataDefence));
        attacker.markAsPlaying(gameID);
        defender.markAsPlaying(gameID);
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
            ReadyClientData clientData) throws IOException, MoveProcessorException{
        if(me.getGameID() == null){
            throw new NullPointerException();
        }
        final UUID gameID = me.getGameID();
        if(!setChosenCards(gameID, side, clientData.getCards())){
            return;
        }

        // А вот тут уже интересно. Считаем ход
        // Получаем разрешенные и выбранные карты и сопоставляем их
        final List<Card> availableCards = getAvailableCards(gameID);
        final List<Card> chosenCards = getChosenCards(gameID);
        final List<Card> cards = chosenCards.stream().filter(availableCards::contains).collect(Collectors.toList());
        // Предыдущий ход, если он был
        final Move previousMove = getPreviousMove(gameID);
        // Составляем базовую информацию о ходе
        final Move move = previousMove != null ?
                                new Move(previousMove) : // Если это не первый ход, за основу предыдущий
                                new Move(gameplaySettings.getMaxCastleHP()); // Иначе первый инициализируем
        // Рассчет хода
        MoveProcessor.processMove(cards, move);
        // Запишем результаты в кэш
        if(!gameMovesCache.containsKey(gameID)){
            // Если вдруг каким то странным и непонятным образом не будет записи
            gameMovesCache.put(gameID, new ConcurrentLinkedDeque<>());
        }
        gameMovesCache.get(gameID).offer(move);

        // Собираем результаты хода
        final List<UnitData> units =  move.getUnits()
                .stream()
                .map(p ->
                    new UnitData(
                            p.getUnitID(),
                            p.getAssotiatedCardAlias(),
                            p.getMaxHP(),
                            p.getCurrentHP(),
                            new PointData(p.getStartPoint())))
                .collect(Collectors.toList());
        final List<ActionData> actions = move.getActions()
                .stream()
                .map(p ->
                    new ActionData(
                            p.getActor().getUnitID(),
                            p.getActionType().toString(),
                            p.getActionParams(),
                            p.getBeginOffset(),
                            p.getEndOffset()))
                .collect(Collectors.toList());
        final List<UnitData> alive =  move.getAliveUnits()
                .stream()
                .map(p ->
                    new UnitData(
                            p.getUnitID(),
                            p.getAssotiatedCardAlias(),
                            p.getMaxHP(),
                            p.getCurrentHP(),
                            new PointData(p.getStartPoint())))
                .collect(Collectors.toList());

        // Запись в базу информации о ходе(следующему пригодится)
        completeMove(gameID, move, units, alive, actions);
        // Возвращаем клиенту все что насчитали
        final String answer = serializeMoveResult(gameID, move, units, actions);
        me.send(answer);
        other.send(answer);
    }

    private void processRenderComplete(
            IPlayerConnection me,
            IPlayerConnection other,
            Side side,
            @SuppressWarnings("unused") RenderCompleteClientData data) throws IOException{
        if(me.getGameID() == null){
            throw new NullPointerException();
        }
        final UUID gameID = me.getGameID();
        // Синхронная вставка, узкое место.
        if(!setRenderComplete(gameID, side)){
            return;
        }
        // Ход, по которому только что было отрендерено
        // Он сохраняется, с учетом него будет считаться след. ход
        // Удалится после записи след. хода в кэш
        final Move move = getCurrentMove(gameID);
        if(move == null){
            throw new NullPointerException("Потерялся последний ход");
        }

        if(move.getCurrentCastleHP() <= 0){
            // Разбили замок, завершаем игру с победой атакующего
            completeMatch(gameID, GameDbService.WinnerType.ATTACKER, me, other);
        } else if(move.getCurrentMove() >= gameplaySettings.getMaxMovesCount()){
            // Прошло максимальное количество ходов, завершаем игру с победой защиты
            completeMatch(gameID, GameDbService.WinnerType.DEFENDER, me, other);
        } else {
            // Продолжаем игру, выделяем карточки для следующего хода
            final List<Card> forAttack = cardManager
                    .getCardsForMove(Side.ATTACKER, move.getCurrentMove() + 1);
            final List<Card> forDefence = cardManager
                    .getCardsForMove(Side.DEFENDER, move.getCurrentMove() + 1);
            createMove(gameID, forAttack, forDefence);

            final List<CardData> forAttackCardData =
                    forAttack.stream().map(p -> new CardData(p.getAlias())).collect(Collectors.toList());
            final CardsForNextMoveGameServerData cardAttackData =
                    new CardsForNextMoveGameServerData(gameID, forAttackCardData);

            final List<CardData> forDefenceCardData =
                    forAttack.stream().map(p -> new CardData(p.getAlias())).collect(Collectors.toList());
            final CardsForNextMoveGameServerData cardDefenceData =
                    new CardsForNextMoveGameServerData(gameID, forDefenceCardData);

            if(side == Side.ATTACKER){
                me.send(mapper.writeValueAsString(cardAttackData));
                other.send(mapper.writeValueAsString(cardDefenceData));
            } else {
                other.send(mapper.writeValueAsString(cardAttackData));
                me.send(mapper.writeValueAsString(cardDefenceData));
            }
        }
    }

    private void processClose(
            IPlayerConnection me,
            IPlayerConnection other,
            @SuppressWarnings("unused") CloseClientData data){
        try{
            me.close();
            other.close();
        } catch (IOException e) {
            e.printStackTrace();
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
            if(me.getConnectionStatus() != IPlayerConnection.ConnectionStatus.PLAYING
                    || me.getConnectionStatus() != IPlayerConnection.ConnectionStatus.PLAYING){
                throw new InvalidPlayerConnectionStateException();
            }

            final GameClientData baseData = mapper.readValue(message, GameClientData.class);
            if(baseData.getStatus().equals(GameClientData.SEND_CHAT_MESSAGE)){
                final MessageClientData data = mapper.readValue(message, MessageClientData.class);
                processChatMessage(me, other, data);
            } else if(baseData.getStatus().equals(GameClientData.READY)) {
                final ReadyClientData data = mapper.readValue(message, ReadyClientData.class);
                processReady(me, other, side, data);
            } else if(baseData.getStatus().equals(GameClientData.RENDER_COMPLETE)) {
                final RenderCompleteClientData data = mapper.readValue(message, RenderCompleteClientData.class);
                processRenderComplete(me, other, side, data);
            } else if(baseData.getStatus().equals(GameClientData.CLOSE)) {
                final CloseClientData data = mapper.readValue(message, CloseClientData.class);
                processClose(me, other, data);
            }

        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            criticalExceptionRaised(e, me, other);
        }
    }

    private void close(
            @SuppressWarnings("unused") IPlayerConnection me,
            IPlayerConnection other){
        try {
            final UUID gameID = other.getGameID();
            other.close();
            chosenCardsCache.remove(gameID);
            availableCardsCache.remove(gameID);
            currentMoveStates.remove(gameID);
            gameMovesCache.remove(gameID);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void startGame(IPlayerConnection attacker, IPlayerConnection defender){
        try {
            startMatch(attacker, defender);

            // При получении информации с клиента будет вызываться receive,
            // Причем, за счет замыканий, в методе будут доступны оба игрока
            attacker.onReceive((conn, str) -> receive(conn, defender, Side.ATTACKER, str));
            defender.onReceive((conn, str) -> receive(conn, attacker, Side.DEFENDER, str));
            attacker.onClose((conn, status) -> close(conn, defender));
            defender.onClose((conn, status) -> close(conn, attacker));
        } catch (IOException
                | NullPointerException
                | SQLException e) {
            criticalExceptionRaised(e, attacker, defender);
        }
    }

    private void criticalExceptionRaised(
            Exception e,
            IPlayerConnection me,
            IPlayerConnection other){
        e.printStackTrace();
        final UUID gameID = me.getGameID() != null ?
                me.getGameID() :
                UUID.fromString("00000000-0000-0000-0000-000000000000");
        try {
            final ErrorGameServerData data = new ErrorGameServerData(gameID);
            me.send(mapper.writeValueAsString(data));
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") IOException jpe){
            jpe.printStackTrace();
        }
        me.markAsErrorable();
        other.markAsErrorable();
        me.onReceive(null);
        other.onClose(null);
        me.onReceive(null);
        other.onReceive(null);
        chosenCardsCache.remove(gameID);
        availableCardsCache.remove(gameID);
        currentMoveStates.remove(gameID);
        gameMovesCache.remove(gameID);
        try {
            me.close();
            other.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
