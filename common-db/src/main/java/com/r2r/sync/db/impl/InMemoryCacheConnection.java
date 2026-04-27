package com.r2r.sync.db.impl;

import com.r2r.sync.db.CacheConnection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InMemoryCacheConnection implements CacheConnection using a ConcurrentHashMap.
 */
public class InMemoryCacheConnection implements CacheConnection {
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Override
    public void set(String key, String value, long ttlSeconds) {
        cache.put(key, value);
    }

    @Override
    public String get(String key) {
        return cache.get(key);
    }

    @Override
    public boolean exists(String key) {
        return cache.containsKey(key);
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
    }
}
