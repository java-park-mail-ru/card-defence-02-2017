package com.kvteam.backend.services;

import com.kvteam.backend.gameplay.GameplaySettings;
import com.kvteam.backend.websockets.IPlayerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * Created by maxim on 23.05.17.
 */
@Service
public class TimeoutService {
    private static class TimeoutConnectionPair {
        private IPlayerConnection attacker;
        private IPlayerConnection defender;
        private long timeout;
        TimeoutConnectionPair(long timeout,
                              IPlayerConnection attacker,
                              IPlayerConnection defender) {
            this.attacker = attacker;
            this.defender = defender;
            this.timeout = timeout;
        }
        public IPlayerConnection getAttacker(){
            return attacker;
        }
        public IPlayerConnection getDefender(){
            return defender;
        }
        public long getTimeout(){
            return timeout;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof TimeoutConnectionPair
                    && ((TimeoutConnectionPair) o).attacker == attacker
                    && ((TimeoutConnectionPair) o).defender == defender;
        }

        @Override
        public int hashCode() {
            int result = attacker != null ? attacker.hashCode() : 0;
            result = 31 * result + (defender != null ? defender.hashCode() : 0);
            result = 31 * result + (int) (timeout ^ (timeout >>> 32));
            return result;
        }
    }
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int TIMEOUT_CHECK_STEP = 1;

    private ScheduledExecutorService timeoutExecutorService;
    private LinkedList<TimeoutConnectionPair> timeouts;
    private LinkedList<TimeoutConnectionPair> toInsert;
    private LinkedList<TimeoutConnectionPair> toDelete;
    private Semaphore addSemaphore;
    private Semaphore deleteSemaphore;

    private BiConsumer<IPlayerConnection, IPlayerConnection> timeoutCallback;

    private long offset;

    public TimeoutService(GameplaySettings gameplaySettings) {
        this.offset = gameplaySettings.getReadyStateTimeout();
        timeoutCallback = null;
        if (this.offset != -1) {
            timeoutExecutorService = Executors.newSingleThreadScheduledExecutor();
            timeoutExecutorService.scheduleWithFixedDelay(
                    this::checkTimeouts,
                    TIMEOUT_CHECK_STEP,
                    TIMEOUT_CHECK_STEP,
                    TimeUnit.SECONDS);
            timeouts = new LinkedList<>();
            toInsert = new LinkedList<>();
            toDelete = new LinkedList<>();
            addSemaphore = new Semaphore(1);
            deleteSemaphore = new Semaphore(1);
        }
    }

    @PreDestroy
    private void joinThreads(){
        if(timeoutExecutorService != null) {
            timeoutExecutorService.shutdown();
        }
    }

    public void setTimeoutCallback(BiConsumer<IPlayerConnection, IPlayerConnection> clbck) {
        timeoutCallback = clbck;
    }

    public void tryAddToTimeouts(IPlayerConnection attacker,
                                 IPlayerConnection defender) {
        if (timeoutExecutorService != null && timeouts != null) {
            final long timeout = System.currentTimeMillis() + offset * 1000;
            try {
                addSemaphore.acquire();
                toInsert.addLast(new TimeoutConnectionPair(timeout, attacker, defender));
            } catch (InterruptedException e) {
                logger.error("lock exception", e);
            } finally {
                addSemaphore.release();
            }
        }
    }

    public void tryRemoveFromTimeouts(
            IPlayerConnection attacker,
            IPlayerConnection defender) {
        if(toDelete != null) {
            try {
                deleteSemaphore.acquire();
                toDelete.addLast(new TimeoutConnectionPair(0, attacker, defender));
            } catch (InterruptedException e) {
                logger.error("lock exception", e);
            } finally {
                deleteSemaphore.release();
            }
        }
    }

    private void checkTimeouts() {
        addIfNeeded();
        deleteIfNeeded();

        final LinkedList<TimeoutConnectionPair> callClose = new LinkedList<>();
        if(!timeouts.isEmpty()) {
            TimeoutConnectionPair pair = timeouts.poll();
            final TimeoutConnectionPair firstPair = pair;
            do {
                if(pair.getTimeout() < System.currentTimeMillis()) {
                    callClose.add(pair);
                } else {
                    timeouts.addLast(pair);
                }
            }while(!timeouts.isEmpty() && !(pair = timeouts.peek()).equals(firstPair));
        }
        if(!callClose.isEmpty()) {
            callClose.forEach(this::callTimeouts);
        }
    }

    private void callTimeouts(TimeoutConnectionPair p) {
        // Если хотя бы один захвачен,
        // значит они в обработке и таймаут не актуален
        if(p.getAttacker().getSemaphore().tryAcquire()) {
            try {
                if( p.getDefender().getSemaphore().tryAcquire() ) {
                    try {
                        timeoutCallback.accept(p.getAttacker(), p.getDefender());
                    } catch (RuntimeException e) {
                        logger.error("runtime", e);
                    } finally {
                        p.getDefender().getSemaphore().release();
                    }
                }
            } finally {
                p.getAttacker().getSemaphore().release();
            }
        }
    }

    private void addIfNeeded() {
        try {
            addSemaphore.acquire();
            timeouts.addAll(toInsert);
            toInsert.clear();
        } catch (InterruptedException e) {
            logger.error("lock exception", e);
        } finally {
            addSemaphore.release();
        }
    }

    private void deleteIfNeeded() {
        try {
            // Семафор на таймаутс уже должен стоять
            deleteSemaphore.acquire();
            timeouts.removeAll(toDelete);
            toDelete.clear();
        } catch (InterruptedException e) {
            logger.error("lock exception", e);
        } finally {
            deleteSemaphore.release();
        }
    }
}
