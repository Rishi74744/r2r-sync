package com.r2r.sync.repository.impl;

import com.r2r.sync.db.DBConnection;
import com.r2r.sync.enums.EventStatus;
import com.r2r.sync.entity.SyncEventEntity;
import com.r2r.sync.repository.EventRepository;
import java.util.ArrayList;
import java.util.List;

public class EventRepositoryImpl implements EventRepository {
    private final DBConnection db;
    private static final String COLLECTION = "events";

    public EventRepositoryImpl(DBConnection db) {
        this.db = db;
    }

    @Override
    public void save(SyncEventEntity event) {
        String id = db.save(COLLECTION, event);
        event.setEventId(id);
    }

    @Override
    public void updateStatus(String eventId, EventStatus status) {
        db.findById(COLLECTION, eventId, SyncEventEntity.class).ifPresent(event -> {
            event.setStatus(status);
            event.setUpdatedAt(System.currentTimeMillis());
            db.save(COLLECTION, event);
        });
    }

    @Override
    public void incrementRetry(String eventId) {
        db.findById(COLLECTION, eventId, SyncEventEntity.class).ifPresent(event -> {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setUpdatedAt(System.currentTimeMillis());
            db.save(COLLECTION, event);
        });
    }

    @Override
    public void incrementReconcile(String eventId) {
        db.findById(COLLECTION, eventId, SyncEventEntity.class).ifPresent(event -> {
            event.setReconcileCount(event.getReconcileCount() + 1);
            event.setUpdatedAt(System.currentTimeMillis());
            db.save(COLLECTION, event);
        });
    }

    @Override
    public void resetRetries(String eventId) {
        db.findById(COLLECTION, eventId, SyncEventEntity.class).ifPresent(event -> {
            event.setRetryCount(0);
            event.setUpdatedAt(System.currentTimeMillis());
            db.save(COLLECTION, event);
        });
    }

    @Override
    public SyncEventEntity getEvent(String eventId) {
        return db.findById(COLLECTION, eventId, SyncEventEntity.class).orElse(null);
    }

    @Override
    public List<SyncEventEntity> findAll() {
        return new ArrayList<>(db.findAll(COLLECTION, SyncEventEntity.class));
    }

    public DBConnection getDb() {
        return db;
    }
}
