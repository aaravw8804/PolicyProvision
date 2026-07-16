package com.telusko.policyprovision.repository;

import com.telusko.policyprovision.model.AuditRecord;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pure in-memory Audit store, backed by ConcurrentHashMap. Audits are
 * append-only - there is deliberately no update or delete here.
 */
@Repository
public class AuditRepository {

    private final Map<String, AuditRecord> store = new ConcurrentHashMap<>();

    public AuditRecord save(AuditRecord record) {
        store.put(record.getId(), record);
        return record;
    }

    public List<AuditRecord> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(AuditRecord::getId))
                .toList();
    }

    public List<AuditRecord> findByProposalId(String proposalId) {
        return store.values().stream()
                .filter(a -> a.getProposalId().equals(proposalId))
                .toList();
    }
}
