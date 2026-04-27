package com.r2r.sync.factory;

import com.r2r.sync.connector.Connector;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectorFactory {
    private final Map<String, Connector> connectors = new ConcurrentHashMap<>();

    public void registerConnector(String connectorId, Connector connector) {
        connectors.put(connectorId, connector);
    }

    public Connector getConnector(String connectorId) {
        Connector connector = connectors.get(connectorId);
        if (Objects.isNull(connector)) {
            throw new RuntimeException("No connector found for id: " + connectorId);
        }
        return connector;
    }
}
