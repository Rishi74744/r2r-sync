package com.r2r.sync.events;

import com.r2r.sync.service.RetryScheduler;
import com.r2r.sync.dto.ApiResponseDTO;
import com.r2r.sync.dto.RecordDTO;
import com.r2r.sync.dto.SyncEventDTO;
import com.r2r.sync.enums.EventStatus;
import com.r2r.sync.lib.common.Constants;
import com.r2r.sync.lib.service.*;
import com.r2r.sync.repository.RecordRepository;
import com.r2r.sync.repository.EventRepository;
import com.r2r.sync.repository.ConnectorConfigRepository;
import com.r2r.sync.repository.AuditRepository;
import com.r2r.sync.entity.RecordEntity;
import com.r2r.sync.entity.ConnectorConfigEntity;
import com.r2r.sync.entity.AuditEntity;

import java.util.UUID;

/**
 * ConnectorWorker is the core orchestrator for executing a synchronization
 * event.
 * It is stateless and fetches per-connector configuration from the repository
 * dynamically.
 */
public class ConnectorWorker {
    private final IdempotencyService idempotencyService;
    private final VersionService versionService;
    private final SyncRateLimiter rateLimiter;
    private final ApiExecutionService apiExecutionService;
    private final ResultProcessor resultProcessor;
    private final RecordRepository recordRepository;
    private final EventRepository eventRepository;
    private final ConnectorConfigRepository configRepository;
    private final AuditRepository auditRepository;
    private final CircuitBreaker circuitBreaker;
    private final BackpressureService backpressureController;
    private final RetryScheduler retryScheduler;

    private static final long THROTTLE_RETRY_DELAY_MS = 2000;

    public ConnectorWorker(IdempotencyService idempotencyService, VersionService versionService,
            SyncRateLimiter rateLimiter, ApiExecutionService apiExecutionService,
            ResultProcessor resultProcessor, RecordRepository recordRepository,
            EventRepository eventRepository, ConnectorConfigRepository configRepository,
            AuditRepository auditRepository,
            CircuitBreaker circuitBreaker, BackpressureService backpressureController,
            RetryScheduler retryScheduler) {
        this.idempotencyService = idempotencyService;
        this.versionService = versionService;
        this.rateLimiter = rateLimiter;
        this.apiExecutionService = apiExecutionService;
        this.resultProcessor = resultProcessor;
        this.recordRepository = recordRepository;
        this.eventRepository = eventRepository;
        this.configRepository = configRepository;
        this.auditRepository = auditRepository;
        this.circuitBreaker = circuitBreaker;
        this.backpressureController = backpressureController;
        this.retryScheduler = retryScheduler;
    }

    public void process(SyncEventDTO event) {
        ConnectorConfigEntity config = configRepository.findByConnectorId(event.getConnectorId())
                .orElse(getDefaultConfig(event.getConnectorId()));

        // STEP 1: Backpressure check
        if (!backpressureController.tryAcquire(event.getConnectorId(), config.getMaxConcurrency())) {
            logAudit(event.getEventId(), "THROTTLED", "BACKPRESSURE",
                    "Concurrency limit reached: " + config.getMaxConcurrency());
            retryScheduler.scheduleRetry(event, System.currentTimeMillis() + THROTTLE_RETRY_DELAY_MS);
            return;
        }

        try {
            // STEP 2: Idempotency check
            if (idempotencyService.isDuplicate(event.getEventId())) {
                logAudit(event.getEventId(), "SKIPPED", "DUPLICATE", "Event already processed or in progress");
                eventRepository.updateStatus(event.getEventId(), EventStatus.SUCCESS);
                return;
            }

            // STEP 3: Version check
            RecordEntity entity = recordRepository.findById(event.getRecordId()).orElse(null);
            RecordDTO recordDTO = entity != null
                    ? RecordDTO.builder().id(entity.getId()).version(entity.getVersion()).build()
                    : null;

            if (versionService.isStale(event, recordDTO)) {
                logAudit(event.getEventId(), "SKIPPED", "STALE",
                        "Event version (" + event.getVersion() + ") is stale compared to record version ("
                                + (recordDTO != null ? recordDTO.getVersion() : "null") + ")");
                eventRepository.updateStatus(event.getEventId(), EventStatus.SUCCESS);
                idempotencyService.markProcessed(event.getEventId());
                return;
            }

            // STEP 4: Rate limiting
            if (!rateLimiter.tryAcquire(event.getConnectorId(), config.getRateLimitPerSecond())) {
                logAudit(event.getEventId(), "THROTTLED", "RATE_LIMIT",
                        "Rate limit hit: " + config.getRateLimitPerSecond() + " req/s");
                retryScheduler.scheduleRetry(event, System.currentTimeMillis() + THROTTLE_RETRY_DELAY_MS);
                return;
            }

            // STEP 5: State Transition
            eventRepository.updateStatus(event.getEventId(), EventStatus.IN_PROGRESS);

            // STEP 6: API Execution
            ApiResponseDTO response = circuitBreaker.execute(event.getConnectorId(),
                    () -> apiExecutionService.execute(event));

            // STEP 7: Result Processing
            resultProcessor.handle(event, response, config);

        } catch (Exception e) {
            System.err.println("Error processing event " + event.getEventId() + ": " + e.getMessage());
            resultProcessor.handle(event,
                    ApiResponseDTO.builder().status(Constants.STATUS_FAILURE).retryable(true).build(), config);
        } finally {
            backpressureController.release(event.getConnectorId());
        }
    }

    private void logAudit(String eventId, String status, String action, String details) {
        auditRepository.save(AuditEntity.builder()
                .id(UUID.randomUUID().toString())
                .eventId(eventId)
                .status(status)
                .action(action)
                .details(details)
                .timestamp(System.currentTimeMillis())
                .build());
    }

    private ConnectorConfigEntity getDefaultConfig(String connectorId) {
        return ConnectorConfigEntity.builder()
                .connectorId(connectorId)
                .rateLimitPerSecond(1)
                .maxConcurrency(Constants.DEFAULT_BACKPRESSURE_LIMIT)
                .maxRetries(Constants.DEFAULT_MAX_RETRIES)
                .maxReconcileAttempts(Constants.DEFAULT_MAX_RECONCILE_ATTEMPTS)
                .retryInitialBackoffMs(Constants.DEFAULT_INITIAL_BACKOFF_MS)
                .build();
    }
}
