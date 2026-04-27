package com.r2r.sync.db.impl;

import com.r2r.sync.db.CacheConnection;
import com.r2r.sync.db.DBConnection;

/**
 * DBConnector is a factory that creates DB and Cache connections based on configuration.
 */
public class DBConnector {
    
    /**
     * Creates a DB connection based on the provided type.
     */
    public static DBConnection createDBConnection(String type) {
        if ("in-memory".equalsIgnoreCase(type)) {
            return new InMemoryDBConnection();
        }
        throw new UnsupportedOperationException("DB type not supported: " + type);
    }

    /**
     * Creates a Cache connection based on the provided type.
     */
    public static CacheConnection createCacheConnection(String type) {
        if ("in-memory".equalsIgnoreCase(type)) {
            return new InMemoryCacheConnection();
        }
        throw new UnsupportedOperationException("Cache type not supported: " + type);
    }
}
