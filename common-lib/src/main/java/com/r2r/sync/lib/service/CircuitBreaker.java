package com.r2r.sync.lib.service;

import java.util.function.Supplier;

public interface CircuitBreaker {
    <T> T execute(String connectorId, Supplier<T> operation);
}
