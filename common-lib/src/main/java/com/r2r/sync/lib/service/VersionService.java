package com.r2r.sync.lib.service;

import com.r2r.sync.dto.RecordDTO;
import com.r2r.sync.dto.SyncEventDTO;

public interface VersionService {
    boolean isStale(SyncEventDTO event, RecordDTO recordDTO);
}
