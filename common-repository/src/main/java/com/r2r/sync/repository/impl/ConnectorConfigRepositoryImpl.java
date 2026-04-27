package com.r2r.sync.repository.impl;

import com.r2r.sync.db.DBConnection;
import com.r2r.sync.entity.ConnectorConfigEntity;
import com.r2r.sync.repository.ConnectorConfigRepository;
import java.util.Optional;

public class ConnectorConfigRepositoryImpl implements ConnectorConfigRepository {
    private final DBConnection db;
    private static final String COLLECTION = "configs";

    public ConnectorConfigRepositoryImpl(DBConnection db) {
        this.db = db;
    }

    @Override
    public void save(ConnectorConfigEntity config) {
        db.save(COLLECTION, config);
    }

    @Override
    public Optional<ConnectorConfigEntity> findByConnectorId(String connectorId) {
        return db.findById(COLLECTION, connectorId, ConnectorConfigEntity.class);
    }
}
