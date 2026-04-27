package com.r2r.sync.events;

import com.r2r.sync.connector.Connector;
import com.r2r.sync.dto.ApiResponseDTO;
import com.r2r.sync.dto.SyncEventDTO;
import com.r2r.sync.factory.ConnectorFactory;

public class ApiExecutionService {
    private final ConnectorFactory connectorFactory;

    public ApiExecutionService(ConnectorFactory connectorFactory) {
        this.connectorFactory = connectorFactory;
    }

    public ApiResponseDTO execute(SyncEventDTO event) {
        Connector connector = connectorFactory.getConnector(event.getConnectorId());
        return connector.send(event);
    }
}
