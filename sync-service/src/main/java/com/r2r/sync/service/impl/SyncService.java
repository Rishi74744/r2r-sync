package com.r2r.sync.service.impl;

import com.r2r.sync.dto.SyncEventDTO;
import com.r2r.sync.entity.AuditEntity;
import com.r2r.sync.entity.RecordEntity;
import com.r2r.sync.entity.SyncEventEntity;
import com.r2r.sync.events.EventCreator;
import com.r2r.sync.events.EventDispatcher;
import com.r2r.sync.repository.AuditRepository;
import com.r2r.sync.repository.EventRepository;
import com.r2r.sync.repository.RecordRepository;
import java.util.UUID;

/**
 * SyncService is the primary entry point for triggering synchronization tasks.
 * it manages the flow from record retrieval to event creation and dispatching.
 */
public class SyncService {
    private final RecordRepository recordRepository;
    private final EventRepository eventRepository;
    private final EventCreator eventCreator;
    private final EventDispatcher eventDispatcher;
    private final AuditRepository auditRepository;

    public SyncService(RecordRepository recordRepository, EventRepository eventRepository, 
                       EventCreator eventCreator, EventDispatcher eventDispatcher,
                       AuditRepository auditRepository) {
        this.recordRepository = recordRepository;
        this.eventRepository = eventRepository;
        this.eventCreator = eventCreator;
        this.eventDispatcher = eventDispatcher;
        this.auditRepository = auditRepository;
    }

    /**
     * Triggers a synchronization task for a specific record and connector.
     */
    public void triggerSync(String recordId, String connectorId) {
        RecordEntity record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found: " + recordId));

        // Create entity for persistence
        SyncEventEntity entity = eventCreator.createEvent(record, connectorId);
        eventRepository.save(entity);
        
        // Log initial audit
        auditRepository.save(AuditEntity.builder()
                .id(UUID.randomUUID().toString())
                .eventId(entity.getEventId())
                .status("PENDING")
                .action("EVENT_CREATED")
                .details("Initial synchronization event created for record " + recordId)
                .timestamp(System.currentTimeMillis())
                .build());
        
        // Map to domain object for execution
        SyncEventDTO event = mapToDomain(entity);
        eventDispatcher.dispatch(event);
    }

    private SyncEventDTO mapToDomain(SyncEventEntity entity) {
        return SyncEventDTO.builder()
                .eventId(entity.getEventId())
                .recordId(entity.getRecordId())
                .connectorId(entity.getConnectorId())
                .payload(entity.getPayload())
                .status(entity.getStatus())
                .retryCount(entity.getRetryCount())
                .version(entity.getVersion())
                .build();
    }
}
