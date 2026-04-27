package com.r2r.sync.service;

import com.r2r.sync.dto.SyncEventDTO;

import java.util.function.Consumer;

public interface RetryScheduler {
    void setHandler(Consumer<SyncEventDTO> handler);
    void scheduleRetry(SyncEventDTO event, long targetTimeMillis);
}
