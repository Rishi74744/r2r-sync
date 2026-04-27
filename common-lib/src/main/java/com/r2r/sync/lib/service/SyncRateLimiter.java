package com.r2r.sync.lib.service;

public interface SyncRateLimiter {
    boolean tryAcquire(String connectorId, int rateLimitPerSecond);
    void reset(String connectorId);
}
