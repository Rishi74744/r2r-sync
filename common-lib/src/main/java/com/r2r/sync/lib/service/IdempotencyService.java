package com.r2r.sync.lib.service;

public interface IdempotencyService {
    boolean isDuplicate(String eventId);
    void markProcessed(String eventId);
    void removeMark(String eventId);
}
