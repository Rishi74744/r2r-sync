package com.r2r.sync.service.impl;

import com.r2r.sync.dto.SyncEventDTO;
import com.r2r.sync.service.RetryScheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * RetrySchedulerImpl manages the execution of delayed synchronization events.
 * it uses a ScheduledExecutorService to simulate a persistent background scheduler.
 */
public class RetrySchedulerImpl implements RetryScheduler {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Consumer<SyncEventDTO> retryHandler;

    /**
     * Sets the handler (callback) that will be called when a retry is triggered.
     */
    @Override
    public void setHandler(Consumer<SyncEventDTO> handler) {
        this.retryHandler = handler;
    }

    /**
     * Schedules a synchronization event to be re-processed at a specific future time.
     */
    @Override
    public void scheduleRetry(SyncEventDTO event, long targetTimeMillis) {
        long delay = targetTimeMillis - System.currentTimeMillis();
        if (delay < 0) delay = 0;

        System.out.println("Scheduling retry for event " + event.getEventId() + " in " + delay + "ms");
        
        scheduler.schedule(() -> {
            if (retryHandler != null) {
                retryHandler.accept(event);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
}
