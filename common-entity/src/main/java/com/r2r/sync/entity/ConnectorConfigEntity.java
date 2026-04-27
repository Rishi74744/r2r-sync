package com.r2r.sync.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectorConfigEntity {
    private String connectorId;
    private int rateLimitPerSecond;
    private int maxConcurrency;
    private int maxRetries;
    private int maxReconcileAttempts;
    private long retryInitialBackoffMs;
}
