package com.kvteam.backend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvteam.backend.dataformats.*;
import com.kvteam.backend.exceptions.MoveProcessorException;
import com.kvteam.backend.gameplay.*;
import com.kvteam.backend.websockets.IPlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Created by maxim on 26.03.17.
 */
@SuppressWarnings({"OverlyBroadThrowsClause", "Duplicates"})
@Service
public class GameService {
    @SuppressWarnings("PublicField")
    private static class MatchContext {
        @SuppressWarnings("InnerClassTooDeeplyNested")
        enum State {
            RENDERING,
            CHOSING_CARDS,
        }

        public ConcurrentLinkedDeque<Move> gameMovesCache = new ConcurrentLinkedDeque<>();
        public MovePlayersReadyState readyState = new MovePlayersReadyState();
        public List<Card> availableCardsCache;
        public List<Card> chosenCardsCache;
        public State state;
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ObjectMapper mapper;
    private GameDbService dbService;
    private CardManager cardManager;
    private GameplaySettings gameplaySettings;
    private TimeoutService timeoutService;

    private ExecutorService dbExecutorService;
    @Value("db_thread_count")
    private String dbThreadCountStr;

    private Map<UUID, MatchContext> matchContexts;

    public GameService(
            ObjectMapper mapper,
            GameDbService dbService,
            CardManager cardManager,
            GameplaySettings gameplaySettings,
            TimeoutService timeoutService){
        this.mapper = mapper;
        this.dbService = dbService;
        this.cardManager = cardManager;
        this.gameplaySettings = gameplaySettings;
        this.timeoutService = timeoutService;

        try{
            final int dbThreadCount = Integer.parseInt(dbThreadCountStr);
            if (dbThreadCount <= 0) {
                dbExecutorService = Executors.newCachedThreadPool();
            } else if(dbThreadCount == 1) {
                dbExecutorService = Executors.newSingleThreadExecutor();
            } else {
                dbExecutorService = Executors.newFixedThreadPool(dbThreadCount);
            }
        } catch(NumberFormatException e){
            dbExecutorService = Executors.newCachedThreadPool();
        }

        timeoutService.setTimeoutCallback(this::onTimeout);

        matchContexts = new ConcurrentHashMap<>();
    }

    @PreDestroy
    private void joinThreads(){
        dbExecutorService.shutdown();
    }

    private void acquireMeAndOther(IPlayerConnection me,
                                   IPlayerConnection other,
                                   Side side) throws InterruptedException {
        if(side == Side.ATTACKER) {
            me.getSemaphore().acquire();
            other.getSemaphore().acquire();
        } else {
            other.getSemaphore().acquire();
            me.getSemaphore().acquire();
        }
    }

    private void releaseMeAndOther(IPlayerConnection me,
                                   IPlayerConnection other,
                                   Side side) {
        if(side == Side.ATTACKER) {
            me.getSemaphore().release();
            other.getSemaphore().release();
        } else {
            other.getSemaphore().release();
            me.getSemaphore().release();
        }
    }

    private void onTimeout(IPlayerConnection attacker, IPlayerConnection defender) {
        // Локи уже стоят
        final UUID gameID = attacker.getGameID();
        if(gameID == null || !matchContexts.containsKey(gameID)) {
            return;
        }
        final boolean attackerIsReady =
                matchContexts.get(gameID).readyState.getAttackReady();
        final boolean defenderIsReady =
                matchContexts.get(gameID).readyState.getDefenceReady();
        final String timeoutFor =
                (attackerIsReady ? "" : attacker.getUsername() + ' ')
                + (defenderIsReady ? "" : defender.getUsername());
        final String winner;
        final GameDbService.WinnerType winnerType;
        if(attackerIsReady) {
            winner = "attacker";
            winnerType = GameDbService.WinnerType.ATTACKER;
        } else if(defenderIsReady) {
            winner = "defender";
            winnerType = GameDbService.WinnerType.DEFENDER;
        } else {
            winner = "none";
            winnerType = GameDbService.WinnerType.NONE;
        }
        //noinspection OverlyBroadCatchBlock
        try {
            //noinspection ConstantConditions
            final String msg = mapper.writeValueAsString(
                            new TimeoutServerData(gameID, timeoutFor, winner));
            attacker.send(msg);
            defender.send(msg);
        } catch (IOException e) {
            logger.error("sending exception", e);
        }

        try {
            completeMatch(gameID,
                    winnerType,
                    attacker,
                    defender );
        } catch (RuntimeException | IOException e) {
            logger.error("complete match exception", e);
        }
    }

    private boolean setChosenCards(
            UUID gameID,
            Side side,
            List<PositionedCardData> cards) throws JsonProcessingException{
        final String serialized = mapper.writeValueAsString(cards);
        if(side == Side.ATTACKER) {
            dbExecutorService.execute(() -> dbService.setChosenAttackerCards(gameID, serialized));
            matchContexts.get(gameID).readyState.setAttackReady();
        } else {
            dbExecutorService.execute(() -> dbService.setChosenDefenderCards(gameID, serialized));
            matchContexts.get(gameID).readyState.setDefenceReady();
        }
        final List<Card> forCache = cards
                .stream()
                .map(p -> cardManager.getCard(p.getAlias(), p.getPointData().toPoint()))
                .collect(Collectors.toList());
        if(matchContexts.get(gameID).chosenCardsCache == null){
            matchContexts.get(gameID).chosenCardsCache = forCache;
        } else {
            matchContexts.get(gameID).chosenCardsCache.addAll(forCache);
        }
        return matchContexts.get(gameID).readyState.isBothReady();
    }

    private boolean setRenderComplete(
            UUID gameID,
            Side side){
        if(side == Side.ATTACKER) {
            dbExecutorService.execute(() -> dbService.setDefenderRenderComplete(gameID));
            matchContexts.get(gameID).readyState.setAttackRenderComplete();
        } else {
            dbExecutorService.execute(() -> dbService.setAttackerRenderComplete(gameID));
            matchContexts.get(gameID).readyState.setDefenceRenderComplete();
        }
        return matchContexts.get(gameID).readyState.isBothRenderComplete();
    }

    private String serializeMoveResult(
            @NotNull UUID gameID,
            @NotNull Move move,
            @NotNull List<UnitData> myUnits,
            @NotNull List<UnitData> enemyUnits,
            @NotNull List<ActionData> actions) throws JsonProcessingException{
        final MoveResultGameServerData result;
        if(move.getCurrentCastleHP() <= 0
                || move.getCurrentMove() >= gameplaySettings.getMaxMovesCount()){
            result = new GameFinishingServerData(
                    gameID,
                    move.getCurrentMove(),
                    move.getInitialCastleHP(),
                    move.getCurrentCastleHP() <= 0,
                    myUnits,
                    enemyUnits,
                    actions
            );
        } else {
            result = new ContinousGameServerData(
                    gameID,
                    move.getCurrentMove(),
                    move.getInitialCastleHP(),
                    myUnits,
                    enemyUnits,
                    actions
            );
        }
        return mapper.writeValueAsString(result);
    }

    private void separateUnits(
            List<Unit> units,
            List<UnitData> allUnits,
            List<UnitData> attackUnits,
            List<UnitData> defenceUnits){
        for(Unit unit: units){
            final Side unitSide = unit.getSide();
            final UnitData data = new UnitData(
                    unit.getUnitID(),
                    unit.getAssotiatedCardAlias(),
                    unit.getMaxHP(),
                    unit.getCurrentHP(),
                    new PointData(unit.getStartPoint()));
            allUnits.add(data);
            if(unitSide == Side.ATTACKER){
                attackUnits.add(data);
            } else if(unitSide == Side.DEFENDER){
                defenceUnits.add(data);
            }
        }
    }

    @Nullable
    private List<Card> getAvailableCards(UUID gameID){
        if(matchContexts.containsKey(gameID)
                && matchContexts.get(gameID).availableCardsCache != null){
            return matchContexts.get(gameID).availableCardsCache;
        }
        return null;
    }

    @Nullable
    private List<Card> getChosenCards(UUID gameID){
        if(matchContexts.containsKey(gameID)
                && matchContexts.get(gameID).chosenCardsCache != null){
            return matchContexts.get(gameID).chosenCardsCache;
        }
        return null;
    }

    @Nullable
    private Move getPreviousMove(UUID gameID){
        return matchContexts.containsKey(gameID) ?
                matchContexts.get(gameID).gameMovesCache.poll() :
                null;
    }

    @Nullable
    private Move getCurrentMove(UUID gameID){
        return matchContexts.containsKey(gameID) ?
                matchContexts.get(gameID).gameMovesCache.getLast() :
                null;
    }

    private void createMove(
            UUID gameID,
            List<Card> forAttack,
            List<Card> forDefence){
        matchContexts.get(gameID).readyState =  new MovePlayersReadyState();
        // Запоминаются выданные карты в кэше
        final List<Card> availableCards = new ArrayList<>();
        availableCards.addAll(forAttack);
        availableCards.addAll(forDefence);
        matchContexts.get(gameID).availableCardsCache = availableCards;
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
                logger.error("json exception", e);
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
                logger.error("json exception", e);
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
                dbService.updateRating(gameID);
            }catch(SQLException e){
                logger.error("sql exception", e);
            }
        });
        cardManager.deletePool(gameID);
        matchContexts.remove(gameID);
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
        final MatchContext ctx = new MatchContext();
        ctx.state = MatchContext.State.CHOSING_CARDS;
        matchContexts.put(gameID, ctx);

        cardManager.initPool(gameID);
        // Выделяются карты для выбора
        final List<Card> forAttack = cardManager
                .getCardsForMove(gameID, Side.ATTACKER, 1);
        final List<Card> forDefence = cardManager
                .getCardsForMove(gameID, Side.DEFENDER, 1);

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
                gameplaySettings.getReadyStateTimeout(),
                forAttackCardData);
        final GameStartData startDataDefence = new GameStartData(
                gameID,
                attacker.getUsername(),
                "defence",
                gameplaySettings.getMaxMovesCount(),
                gameplaySettings.getMaxCastleHP(),
                gameplaySettings.getReadyStateTimeout(),
                forDefenceCardData);
        attacker.send(mapper.writeValueAsString(startDataAttack));
        defender.send(mapper.writeValueAsString(startDataDefence));
        attacker.markAsPlaying(gameID);
        defender.markAsPlaying(gameID);
        timeoutService.tryAddToTimeouts(attacker, defender);
        logger.info("start_game", attacker.getUsername() + " vs " + defender.getUsername());
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
        if(matchContexts.get(gameID).state != MatchContext.State.CHOSING_CARDS){
            return;
        }
        if(!setChosenCards(gameID, side, clientData.getCards())){
            return;
        }
        timeoutService.tryRemoveFromTimeouts(
                side == Side.ATTACKER ? me : other,
                side == Side.ATTACKER ? other : me);

        // А вот тут уже интересно. Считаем ход
        // Получаем разрешенные и выбранные карты и сопоставляем их
        final List<Card> availableCards = getAvailableCards(gameID);
        final List<Card> chosenCards = getChosenCards(gameID);
        // Опасность исключения NullPointerException оставлена намеренно
        // Если неполучилось достать карточки для данной игры, то ход завершен с ошибкой
        // Исключение будет поймано выше и клиенту отправлен Error пакет
        //noinspection ConstantConditions
        final List<Card> cards = chosenCards.stream().filter(availableCards::contains).collect(Collectors.toList());
        // Предыдущий ход, если он был
        final Move previousMove = getPreviousMove(gameID);
        // Составляем базовую информацию о ходе
        final Move move = previousMove != null ?
                                new Move(previousMove) : // Если это не первый ход, за основу предыдущий
                                new Move(gameplaySettings.getMaxCastleHP()); // Иначе первый инициализируем
        // Рассчет хода
        MoveProcessor.processMove(gameplaySettings, cards, move);
        // Запишем результаты в кэш
        if(!matchContexts.containsKey(gameID)){
            // Если вдруг каким то странным и непонятным образом не будет записи
            matchContexts.put(gameID, new MatchContext());
        }
        matchContexts.get(gameID).gameMovesCache.offer(move);

        // Собираем результаты хода
        final List<UnitData> units = new LinkedList<>();
        final List<UnitData> attackUnits = new LinkedList<>();
        final List<UnitData> defenceUnits = new LinkedList<>();
        separateUnits(move.getUnits(), units, attackUnits, defenceUnits);

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
        matchContexts.get(gameID).state = MatchContext.State.RENDERING;
        // Запись в базу информации о ходе(следующему пригодится)
        completeMove(gameID, move, units, alive, actions);
        // Возвращаем клиенту все что насчитали
        if(side == Side.ATTACKER) {
            me.send(serializeMoveResult(gameID, move, attackUnits, defenceUnits, actions));
            other.send(serializeMoveResult(gameID, move, defenceUnits, attackUnits, actions));
        } else {
            me.send(serializeMoveResult(gameID, move, defenceUnits, attackUnits, actions));
            other.send(serializeMoveResult(gameID, move, attackUnits, defenceUnits, actions));
        }

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
        if(matchContexts.get(gameID).state != MatchContext.State.RENDERING) {
            return;
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
                    .getCardsForMove(gameID, Side.ATTACKER, move.getCurrentMove() + 1);
            final List<Card> forDefence = cardManager
                    .getCardsForMove(gameID, Side.DEFENDER, move.getCurrentMove() + 1);
            createMove(gameID, forAttack, forDefence);

            final List<CardData> forAttackCardData =
                    forAttack.stream().map(p -> new CardData(p.getAlias())).collect(Collectors.toList());
            final CardsForNextMoveGameServerData cardAttackData =
                    new CardsForNextMoveGameServerData(gameID, forAttackCardData);

            final List<CardData> forDefenceCardData =
                    forDefence.stream().map(p -> new CardData(p.getAlias())).collect(Collectors.toList());
            final CardsForNextMoveGameServerData cardDefenceData =
                    new CardsForNextMoveGameServerData(gameID, forDefenceCardData);

            matchContexts.get(gameID).state = MatchContext.State.CHOSING_CARDS;
            if(side == Side.ATTACKER){
                me.send(mapper.writeValueAsString(cardAttackData));
                other.send(mapper.writeValueAsString(cardDefenceData));
                timeoutService.tryAddToTimeouts(me, other);
            } else {
                other.send(mapper.writeValueAsString(cardAttackData));
                me.send(mapper.writeValueAsString(cardDefenceData));
                timeoutService.tryAddToTimeouts(other, me);
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
            logger.error("closing sockets error", e);
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
            acquireMeAndOther(me, other, side);

            if(me.getConnectionStatus() != IPlayerConnection.ConnectionStatus.PLAYING
                    || other.getConnectionStatus() != IPlayerConnection.ConnectionStatus.PLAYING) {
                return;
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
        } finally {
            releaseMeAndOther(me, other, side);
        }
    }

    private void close(
            @SuppressWarnings("unused") IPlayerConnection me,
            IPlayerConnection other,
            Side side){
        try {
            acquireMeAndOther(me, other, side);
            final UUID gameID = other.getGameID();
            other.close();
            if (gameID != null) {
                cardManager.deletePool(gameID);
            }
            matchContexts.remove(gameID);
        } catch(IOException
                | InterruptedException e) {
            logger.error("closing exception", e);
        } finally {
            releaseMeAndOther(me, other, side);
        }
    }

    public void startGame(IPlayerConnection attacker, IPlayerConnection defender){
        try {
            attacker.getSemaphore().acquire();
            defender.getSemaphore().acquire();

            startMatch(attacker, defender);
            // При получении информации с клиента будет вызываться receive,
            // Причем, за счет замыканий, в методе будут доступны оба игрока
            attacker.onReceive((conn, str) -> receive(conn, defender, Side.ATTACKER, str));
            defender.onReceive((conn, str) -> receive(conn, attacker, Side.DEFENDER, str));
            attacker.onClose((conn, status) -> close(conn, defender, Side.ATTACKER));
            defender.onClose((conn, status) -> close(conn, attacker, Side.DEFENDER));
        } catch (IOException
                | NullPointerException
                | SQLException
                | InterruptedException e) {
            criticalExceptionRaised(e, attacker, defender);
        } finally {
            attacker.getSemaphore().release();
            defender.getSemaphore().release();
        }
    }

    private void criticalExceptionRaised(
            Exception e,
            IPlayerConnection me,
            IPlayerConnection other){
        logger.error("critical exception", e);
        final UUID gameID = me.getGameID() != null ?
                me.getGameID() :
                UUID.fromString("00000000-0000-0000-0000-000000000000");
        try {
            final ErrorGameServerData data = new ErrorGameServerData(gameID);
            me.send(mapper.writeValueAsString(data));
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") IOException jpe){
            logger.error("critical exception", jpe);
        }
        me.markAsErrorable();
        other.markAsErrorable();
        me.onReceive(null);
        other.onClose(null);
        me.onReceive(null);
        other.onReceive(null);
        cardManager.deletePool(gameID);
        matchContexts.remove(gameID);
        try {
            me.close();
            other.close();
        } catch (IOException ex) {
            logger.error("critical exception", ex);
        }
    }

}
