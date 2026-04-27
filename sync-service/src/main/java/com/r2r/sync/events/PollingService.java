package com.r2r.sync.events;

import com.r2r.sync.dto.RecordDTO;
import com.r2r.sync.factory.ConnectorFactory;
import com.r2r.sync.service.impl.RecordService;

import java.util.Map;

/**
 * PollingService simulates the pull model of External-to-Internal synchronization.
 * it periodically checks external connectors for updates and applies them locally.
 */
public class PollingService {
    private final ConnectorFactory connectorFactory;
    private final RecordService recordService;

    public PollingService(ConnectorFactory connectorFactory, RecordService recordService) {
        this.connectorFactory = connectorFactory;
        this.recordService = recordService;
    }

    /**
     * Executes a polling cycle for a specific connector.
     */
    public void pollExternalSystem(String connectorId) {
        System.out.println("Polling external system: " + connectorId);
        
        // In a real system, this would call a list or changes endpoint. For this mock, we simulate receiving a batch of updates.
        
        // Simulating one external update found during polling
        RecordDTO externalUpdate = RecordDTO.builder()
                .id("polled-rec-1")
                .type("USER")
                .data(Map.of("status", "UPDATED_BY_POLLING", "source", connectorId))
                .version(10) // Higher version to ensure it applies
                .build();

        System.out.println("Applying polled update for record: " + externalUpdate.getId());
        
        // Update local state (External -> Internal)
        recordService.updateRecord(externalUpdate.getId(), externalUpdate)
                .orElseGet(() -> recordService.createRecord(externalUpdate));
    }
}
