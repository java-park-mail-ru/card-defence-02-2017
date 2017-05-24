package com.kvteam.backend.services;

import com.kvteam.backend.gameplay.GameplaySettings;
import com.kvteam.backend.websockets.IPlayerConnection;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            return (attacker.toString() + defender.toString()).equals(o.toString());
        }
    }

    private static final int TIMEOUT_CHECK_STEP = 1;

    private ScheduledExecutorService timeoutExecutorService;
    private LinkedList<TimeoutConnectionPair> timeouts;
    private LinkedList<String> toDelete;
    private Semaphore semaphore;
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
            toDelete = new LinkedList<>();
            semaphore = new Semaphore(1);
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
                semaphore.acquire();
                timeouts.addLast(new TimeoutConnectionPair(timeout, attacker, defender));
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                semaphore.release();
            }
        }
    }

    public void tryRemoveFromTimeouts(
            IPlayerConnection attacker,
            IPlayerConnection defender) {
        if(toDelete != null) {
            try {
                deleteSemaphore.acquire();
                toDelete.addLast(attacker.toString() + defender.toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                deleteSemaphore.release();
            }
        }
    }

    private void checkTimeouts() {
        final LinkedList<TimeoutConnectionPair> callClose = new LinkedList<>();
        try {
            semaphore.acquire();
            deleteIfNeeded();

            if(!timeouts.isEmpty()) {
                TimeoutConnectionPair pair = timeouts.poll();
                final TimeoutConnectionPair firstPair = pair;
                do {
                    if(pair.getTimeout() < System.currentTimeMillis()) {
                        callClose.add(pair);
                    } else {
                        timeouts.addLast(pair);
                    }
                }while(!timeouts.isEmpty() && !(pair = timeouts.poll()).equals(firstPair));
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
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
                    } finally {
                        p.getDefender().getSemaphore().release();
                    }
                }
            } finally {
                p.getAttacker().getSemaphore().release();
            }
        }
    }

    private void deleteIfNeeded() {
        try {
            // Семафор на таймаутс уже должен стоять
            deleteSemaphore.acquire();
            timeouts.removeAll(toDelete);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            deleteSemaphore.release();
        }
    }
}
