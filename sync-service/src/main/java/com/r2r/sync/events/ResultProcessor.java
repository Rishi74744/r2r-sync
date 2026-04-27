package com.r2r.sync.events;

import com.r2r.sync.service.RetryScheduler;
import com.r2r.sync.dto.ApiResponseDTO;
import com.r2r.sync.dto.SyncEventDTO;
import com.r2r.sync.enums.EventStatus;
import com.r2r.sync.lib.common.Constants;
import com.r2r.sync.entity.AuditEntity;
import com.r2r.sync.entity.ConnectorConfigEntity;
import com.r2r.sync.lib.service.IdempotencyService;
import com.r2r.sync.repository.AuditRepository;
import com.r2r.sync.repository.EventRepository;
import com.r2r.sync.utility.RetryPolicy;

import java.util.UUID;

/**
 * ResultProcessor handles the post-execution logic using per-connector configuration.
 */
public class ResultProcessor {
    private final EventRepository eventRepository;
    private final RetryPolicy retryPolicy;
    private final RetryScheduler retryScheduler;
    private final ReconciliationService reconciliationService;
    private final AuditRepository auditRepository;
    private final IdempotencyService idempotencyService;

    public ResultProcessor(EventRepository eventRepository, RetryPolicy retryPolicy, 
                           RetryScheduler retryScheduler, ReconciliationService reconciliationService,
                           AuditRepository auditRepository, IdempotencyService idempotencyService) {
        this.eventRepository = eventRepository;
        this.retryPolicy = retryPolicy;
        this.retryScheduler = retryScheduler;
        this.reconciliationService = reconciliationService;
        this.auditRepository = auditRepository;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Handles the outcome of an API call using dynamic configuration for retry thresholds.
     */
    public void handle(SyncEventDTO event, ApiResponseDTO response, ConnectorConfigEntity config) {
        String action;
        String status;
        String details = response.getData() != null ? response.getData().toString() : "";
        boolean isFinished = false;

        if (Constants.STATUS_SUCCESS.equalsIgnoreCase(response.getStatus())) {
            eventRepository.updateStatus(event.getEventId(), EventStatus.SUCCESS);
            event.setStatus(EventStatus.SUCCESS); // Update DTO state
            action = Constants.ACTION_SYNC_SUCCESS;
            status = Constants.STATUS_SUCCESS;
            isFinished = true;
        } else if (response.isRetryable() && event.getRetryCount() < config.getMaxRetries()) {
            eventRepository.updateStatus(event.getEventId(), EventStatus.RETRY);
            event.setStatus(EventStatus.RETRY); // Update DTO state
            eventRepository.incrementRetry(event.getEventId());
            event.setRetryCount(event.getRetryCount() + 1); // Update local DTO state to prevent loops
            
            // Calculate next retry time based on config backoff
            long nextRetry = retryPolicy.nextRetryTime(event.getRetryCount(), config.getRetryInitialBackoffMs());
            retryScheduler.scheduleRetry(event, nextRetry);
            
            action = Constants.ACTION_SYNC_RETRY;
            status = Constants.STATUS_RETRY;
            details += " | Next retry delay: " + (nextRetry - System.currentTimeMillis()) + "ms";
        } else {
            eventRepository.updateStatus(event.getEventId(), EventStatus.RECONCILE);
            event.setStatus(EventStatus.RECONCILE); // Update DTO state
            reconciliationService.reconcile(event, config);
            action = Constants.ACTION_SYNC_EXHAUSTED;
            status = Constants.STATUS_RECONCILE;

            // Only mark as finished if reconciliation didn't reset it to PENDING for another round
            isFinished = (event.getStatus() != EventStatus.PENDING);
        }

        auditRepository.save(AuditEntity.builder()
                .id(UUID.randomUUID().toString())
                .eventId(event.getEventId())
                .action(action)
                .status(status)
                .details(details)
                .timestamp(System.currentTimeMillis())
                .build());

        if (isFinished) {
            idempotencyService.markProcessed(event.getEventId());
        }
    }
}
