package com.r2r.sync.lib.service.impl;

import com.r2r.sync.lib.service.BackpressureService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * BackpressureServiceImpl provides a dynamic semaphore-based concurrency limit per connector.
 */
public class BackpressureServiceImpl implements BackpressureService {
    private final Map<String, Semaphore> semaphores = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String connectorId, int limit) {
        // Dynamically create or retrieve the semaphore for this connector
        Semaphore semaphore = semaphores.computeIfAbsent(connectorId, k -> new Semaphore(limit));
        
        // In a real system, we would handle limit changes by replacing the semaphore but for this mock, we assume fixed limits from the first request.
        return semaphore.tryAcquire();
    }

    @Override
    public void release(String connectorId) {
        Semaphore semaphore = semaphores.get(connectorId);
        if (semaphore != null) {
            semaphore.release();
        }
    }
}
