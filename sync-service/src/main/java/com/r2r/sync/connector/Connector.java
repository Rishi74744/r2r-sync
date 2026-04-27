package com.r2r.sync.connector;

import com.r2r.sync.dto.ApiResponseDTO;
import com.r2r.sync.dto.SyncEventDTO;

/**
 * Connector defines the interface for communicating with external systems.
 */
public interface Connector {
    ApiResponseDTO send(SyncEventDTO event);
}
