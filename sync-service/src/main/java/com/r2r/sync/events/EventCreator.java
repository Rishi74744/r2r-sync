package com.r2r.sync.events;

import com.r2r.sync.enums.EventStatus;
import com.r2r.sync.entity.RecordEntity;
import com.r2r.sync.entity.SyncEventEntity;
import java.util.UUID;

/**
 * EventCreator is responsible for transforming a Record into a SyncEventEntity intent.
 */
public class EventCreator {

    /**
     * Creates a PENDING synchronization event for a given record.
     */
    public SyncEventEntity createEvent(RecordEntity record, String connectorId) {
        return SyncEventEntity.builder()
                .eventId(UUID.randomUUID().toString())
                .recordId(record.getId())
                .connectorId(connectorId)
                .payload(record.getData())
                .status(EventStatus.PENDING)
                .version(record.getVersion())
                .retryCount(0)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();
    }
}
