package com.r2r.sync.lib.service.impl;

import com.r2r.sync.lib.common.Constants;
import com.r2r.sync.lib.exception.SyncException;
import com.r2r.sync.lib.service.CircuitBreaker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * CircuitBreakerImpl protects external API calls by monitoring failure rates.
 * if failures exceed a threshold, it trips to OPEN and fails fast.
 */
public class CircuitBreakerImpl implements CircuitBreaker {
    private enum State { CLOSED, OPEN }
    
    // Tracks the current state (CLOSED/OPEN) for each connector
    private final Map<String, State> states = new ConcurrentHashMap<>();
    
    // Tracks consecutive failures for each connector
    private final Map<String, AtomicInteger> failures = new ConcurrentHashMap<>();
    
    private static final int THRESHOLD = Constants.DEFAULT_CIRCUIT_THRESHOLD;

    /**
     * Executes a supplier operation within a circuit breaker context.
     */
    @Override
    public <T> T execute(String connectorId, Supplier<T> operation) {
        // FAIL FAST: Check if circuit is OPEN
        if (states.getOrDefault(connectorId, State.CLOSED) == State.OPEN) {
            throw new SyncException("Circuit is OPEN for connector: " + connectorId);
        }

        try {
            T result = operation.get();
            // SUCCESS: Reset failures on a successful call
            reset(connectorId);
            return result;
        } catch (Exception e) {
            // FAILURE: Increment failure count and trip if threshold reached
            recordFailure(connectorId);
            throw e;
        }
    }

    private void recordFailure(String connectorId) {
        int count = failures.computeIfAbsent(connectorId, k -> new AtomicInteger(0)).incrementAndGet();
        if (count >= THRESHOLD) {
            System.err.println("Circuit TRIPPED (OPEN) for connector: " + connectorId);
            states.put(connectorId, State.OPEN);
        }
    }

    private void reset(String connectorId) {
        failures.put(connectorId, new AtomicInteger(0));
        states.put(connectorId, State.CLOSED);
    }
}
