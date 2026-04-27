package com.r2r.sync.db;

/**
 * CacheConnection provides a common interface for interacting with a distributed cache like Redis.
 */
public interface CacheConnection {
    void set(String key, String value, long ttlSeconds);
    String get(String key);
    boolean exists(String key);
    void delete(String key);
}
