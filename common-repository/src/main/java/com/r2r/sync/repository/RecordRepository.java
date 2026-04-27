package com.r2r.sync.repository;

import com.r2r.sync.entity.RecordEntity;

import java.util.List;
import java.util.Optional;

public interface RecordRepository {
    RecordEntity save(RecordEntity record);
    Optional<RecordEntity> findById(String id);
    List<RecordEntity> findAll();
    void deleteById(String id);
}
