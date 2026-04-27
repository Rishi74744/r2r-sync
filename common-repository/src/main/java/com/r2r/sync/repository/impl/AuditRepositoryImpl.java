package com.r2r.sync.repository.impl;

import com.r2r.sync.db.DBConnection;
import com.r2r.sync.entity.AuditEntity;
import com.r2r.sync.repository.AuditRepository;
import java.util.ArrayList;
import java.util.List;

public class AuditRepositoryImpl implements AuditRepository {
    private final DBConnection db;
    private static final String COLLECTION = "audits";

    public AuditRepositoryImpl(DBConnection db) {
        this.db = db;
    }

    @Override
    public void save(AuditEntity audit) {
        String id = db.save(COLLECTION, audit);
        audit.setId(id);
    }

    @Override
    public List<AuditEntity> findAll() {
        return new ArrayList<>(db.findAll(COLLECTION, AuditEntity.class));
    }

    @Override
    public List<AuditEntity> findByEventId(String eventId) {
        return db.findAll(COLLECTION, AuditEntity.class).stream()
                .filter(a -> eventId.equals(a.getEventId()))
                .toList();
    }

    public DBConnection getDb() {
        return db;
    }
}
