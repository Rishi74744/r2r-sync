package com.r2r.sync.lib.service.impl;

import com.r2r.sync.lib.service.IdempotencyService;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IdempotencyServiceImpl ensures that the same event is not processed multiple times.
 */
public class IdempotencyServiceImpl implements IdempotencyService {
    // Stores the IDs of successfully processed events
    private final Set<String> processedEvents = ConcurrentHashMap.newKeySet();

    @Override
    public boolean isDuplicate(String eventId) {
        return processedEvents.contains(eventId);
    }

    @Override
    public void markProcessed(String eventId) {
        processedEvents.add(eventId);
    }

    @Override
    public void removeMark(String eventId) {
        processedEvents.remove(eventId);
    }
}
