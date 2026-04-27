package com.r2r.sync.repository;

import com.r2r.sync.entity.AuditEntity;
import java.util.List;

public interface AuditRepository {
    void save(AuditEntity audit);
    List<AuditEntity> findAll();
    List<AuditEntity> findByEventId(String eventId);
}
