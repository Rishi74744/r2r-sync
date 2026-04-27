package com.r2r.sync.repository;

import com.r2r.sync.enums.EventStatus;
import com.r2r.sync.entity.SyncEventEntity;

import java.util.List;

public interface EventRepository {
    void save(SyncEventEntity event);
    void updateStatus(String eventId, EventStatus status);
    void incrementRetry(String eventId);
    void incrementReconcile(String eventId);
    void resetRetries(String eventId);
    SyncEventEntity getEvent(String eventId);
    List<SyncEventEntity> findAll();
}
