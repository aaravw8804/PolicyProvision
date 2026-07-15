package com.telusko.policyprovision.repository;

import com.telusko.policyprovision.model.Proposal;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pure in-memory Proposal store, backed by ConcurrentHashMap.
 */
@Repository
public class ProposalRepository {

    private final Map<String, Proposal> store = new ConcurrentHashMap<>();

    public Proposal save(Proposal proposal) {
        store.put(proposal.getId(), proposal);
        return proposal;
    }

    public Optional<Proposal> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Proposal> findAll() {
        return List.copyOf(store.values());
    }

    public List<Proposal> findByCustomerId(String customerId) {
        return store.values().stream()
                .filter(p -> p.getCustomerId().equals(customerId))
                .toList();
    }

    public void deleteById(String id) {
        store.remove(id);
    }
}
