package com.r2r.sync.db.impl;

import com.r2r.sync.db.DBConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Field;

/**
 * InMemoryDBConnection implements DBConnection using ConcurrentHashMaps and generates IDs.
 */
public class InMemoryDBConnection implements DBConnection {
    private final Map<String, Map<String, Object>> storage = new ConcurrentHashMap<>();

    @Override
    public <T> String save(String collection, T data) {
        String id = getOrGenerateId(data);
        storage.computeIfAbsent(collection, k -> new ConcurrentHashMap<>()).put(id, data);
        return id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> findById(String collection, String id, Class<T> clazz) {
        Map<String, Object> col = storage.get(collection);
        if (col == null) return Optional.empty();
        return Optional.ofNullable((T) col.get(id));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<T> findAll(String collection, Class<T> clazz) {
        Map<String, Object> col = storage.get(collection);
        if (col == null) return Collections.emptyList();
        return (Collection<T>) col.values();
    }

    @Override
    public void updateField(String collection, String id, String field, Object value) {
        // Find object and update field via reflection
        findById(collection, id, Object.class).ifPresent(obj -> {
            try {
                Field f = obj.getClass().getDeclaredField(field);
                f.setAccessible(true);
                f.set(obj, value);
            } catch (Exception e) {
                System.err.println("Failed to update field " + field + " on object " + id);
            }
        });
    }

    @Override
    public void delete(String collection, String id) {
        Map<String, Object> col = storage.get(collection);
        if (col != null) {
            col.remove(id);
        }
    }

    public void clear(String collection) {
        Map<String, Object> col = storage.get(collection);
        if (col != null) {
            col.clear();
        }
    }

    private String getOrGenerateId(Object obj) {
        try {
            // Attempt to find 'id', 'eventId', 'recordId' or 'connectorId' fields via reflection
            for (String fieldName : Arrays.asList("id", "eventId", "recordId", "connectorId")) {
                try {
                    Field f = obj.getClass().getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object val = f.get(obj);
                    if (val != null) {
                        return val.toString();
                    }
                    // If field exists but null, generate and set it
                    String newId = UUID.randomUUID().toString();
                    f.set(obj, newId);
                    return newId;
                } catch (NoSuchFieldException ignored) {}
            }
        } catch (Exception e) {
            System.err.println("Error generating ID: " + e.getMessage());
        }
        return UUID.randomUUID().toString();
    }
}
