package com.telusko.policyprovision.repository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pure in-memory Reference Master store: category name -> list of valid
 * values (e.g. "POLICY_TERM" -> ["10","15","20","25","30"]). Seeded at
 * startup by config.ReferenceDataInitializer.
 */
@Repository
public class ReferenceMasterRepository {

    private final Map<String, List<String>> store = new ConcurrentHashMap<>();

    public void put(String category, List<String> values) {
        store.put(category.toUpperCase(), List.copyOf(values));
    }

    public List<String> get(String category) {
        return store.get(category.toUpperCase());
    }

    public boolean hasCategory(String category) {
        return store.containsKey(category.toUpperCase());
    }

    public boolean isValidValue(String category, String value) {
        List<String> values = get(category);
        return values != null && values.contains(value);
    }

    public Map<String, List<String>> findAll() {
        return Map.copyOf(store);
    }
}
