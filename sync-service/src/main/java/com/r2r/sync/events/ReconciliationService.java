package com.r2r.sync.events;

import com.r2r.sync.dto.SyncEventDTO;
import com.r2r.sync.enums.EventStatus;
import com.r2r.sync.repository.EventRepository;
import com.r2r.sync.entity.ConnectorConfigEntity;
import com.r2r.sync.service.RetryScheduler;
import com.r2r.sync.lib.service.IdempotencyService;

/**
 * ReconciliationService handles the recovery logic when events fail
 * permanently.
 */
public class ReconciliationService {
    private final EventRepository eventRepository;
    private final RetryScheduler retryScheduler;
    private final IdempotencyService idempotencyService;

    public ReconciliationService(EventRepository eventRepository, RetryScheduler retryScheduler,
            IdempotencyService idempotencyService) {
        this.eventRepository = eventRepository;
        this.retryScheduler = retryScheduler;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Attempts to reconcile a failed sync event by incrementing its attempt
     * counter.
     */
    public void reconcile(SyncEventDTO event, ConnectorConfigEntity config) {
        System.out.println("Initiating reconciliation for event: " + event.getEventId());

        eventRepository.incrementReconcile(event.getEventId());
        event.setReconcileCount(event.getReconcileCount() + 1);

        if (event.getReconcileCount() >= config.getMaxReconcileAttempts()) {
            System.out.println(
                    "Reconciliation threshold reached for event: " + event.getEventId() + ". Moving to MANUAL_CHECK.");
            eventRepository.updateStatus(event.getEventId(), EventStatus.MANUAL_CHECK);
        } else {
            // Reset to PENDING and schedule for immediate re-attempt
            System.out.println("Event " + event.getEventId() + " reconciliation attempt " + event.getReconcileCount()
                    + " failed. Resetting for retry round.");

            // CLEAR IDEMPOTENCY to allow re-processing of the same event ID
            idempotencyService.removeMark(event.getEventId());

            eventRepository.updateStatus(event.getEventId(), EventStatus.PENDING);
            eventRepository.resetRetries(event.getEventId());

            // Update local DTO state
            event.setStatus(EventStatus.PENDING);
            event.setRetryCount(0);

            // Schedule immediate re-processing (with a small 2s delay for stability)
            retryScheduler.scheduleRetry(event, System.currentTimeMillis() + 2000);
        }
    }
}
