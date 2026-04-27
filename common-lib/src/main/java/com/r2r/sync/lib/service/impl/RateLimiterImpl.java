package com.r2r.sync.lib.service.impl;

import com.r2r.sync.lib.service.SyncRateLimiter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RateLimiterImpl enforces a minimum interval between requests based on the provided rate limit.
 */
public class RateLimiterImpl implements SyncRateLimiter {
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String connectorId, int rateLimitPerSecond) {
        long now = System.currentTimeMillis();
        long last = lastRequestTime.getOrDefault(connectorId, 0L);
        
        // Calculate interval in milliseconds (e.g. 1 req/sec = 1000ms)
        long intervalMs = 1000 / (rateLimitPerSecond > 0 ? rateLimitPerSecond : 1);

        if (now - last < intervalMs) {
            return false;
        }

        lastRequestTime.put(connectorId, now);
        return true;
    }

    @Override
    public void reset(String connectorId) {
        lastRequestTime.remove(connectorId);
    }
}
