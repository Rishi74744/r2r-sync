package com.r2r.sync.repository.impl;

import com.r2r.sync.db.DBConnection;
import com.r2r.sync.entity.RecordEntity;
import com.r2r.sync.repository.RecordRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecordRepositoryImpl implements RecordRepository {
    private final DBConnection db;
    private static final String COLLECTION = "records";

    public RecordRepositoryImpl(DBConnection db) {
        this.db = db;
    }

    @Override
    public RecordEntity save(RecordEntity record) {
        String id = db.save(COLLECTION, record);
        record.setId(id);
        return record;
    }

    @Override
    public Optional<RecordEntity> findById(String id) {
        return db.findById(COLLECTION, id, RecordEntity.class);
    }

    @Override
    public List<RecordEntity> findAll() {
        return new ArrayList<>(db.findAll(COLLECTION, RecordEntity.class));
    }

    @Override
    public void deleteById(String id) {
        db.delete(COLLECTION, id);
    }
}
