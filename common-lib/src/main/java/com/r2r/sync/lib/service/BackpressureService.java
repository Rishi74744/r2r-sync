package com.r2r.sync.lib.service;

public interface BackpressureService {
    boolean tryAcquire(String connectorId, int limit);
    void release(String connectorId);
}
