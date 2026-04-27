package com.r2r.sync.lib.service.impl;

import com.r2r.sync.dto.RecordDTO;
import com.r2r.sync.dto.SyncEventDTO;
import com.r2r.sync.lib.service.VersionService;

/**
 * VersionServiceImpl prevents out-of-order execution by comparing event versions.
 */
public class VersionServiceImpl implements VersionService {
    
    /**
     * Determines if a synchronization event is stale compared to the current record state.
     */
    @Override
    public boolean isStale(SyncEventDTO event, RecordDTO recordDTO) {
        if (recordDTO == null) return false;
        // If the record has a higher version than the event, the event is obsolete
        return event.getVersion() < recordDTO.getVersion();
    }
}
