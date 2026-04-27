package com.r2r.sync.db;

import java.util.Collection;
import java.util.Optional;

/**
 * DBConnection provides a common interface for interacting with the primary database.
 */
public interface DBConnection {
    <T> String save(String collection, T data);
    <T> Optional<T> findById(String collection, String id, Class<T> clazz);
    <T> Collection<T> findAll(String collection, Class<T> clazz);
    void updateField(String collection, String id, String field, Object value);
    void delete(String collection, String id);
}
