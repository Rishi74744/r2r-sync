package com.r2r.sync.repository;

import com.r2r.sync.entity.ConnectorConfigEntity;
import java.util.Optional;

public interface ConnectorConfigRepository {
    void save(ConnectorConfigEntity config);
    Optional<ConnectorConfigEntity> findByConnectorId(String connectorId);
}
