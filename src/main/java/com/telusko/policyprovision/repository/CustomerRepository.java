package com.telusko.policyprovision.repository;

import com.telusko.policyprovision.model.Customer;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pure in-memory Customer store. Backed by ConcurrentHashMap for thread
 * safety under concurrent requests, per the "no database" requirement.
 * All data is lost on application restart - by design.
 */
@Repository
public class CustomerRepository {

    private final Map<String, Customer> store = new ConcurrentHashMap<>();

    public Customer save(Customer customer) {
        store.put(customer.getId(), customer);
        return customer;
    }

    public Optional<Customer> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    /* public boolean existsById(String id) {
        return store.containsKey(id);
    }*/

    public List<Customer> findAll() {

        return store.values().stream()
                .sorted(Comparator.comparing(Customer::getId))
                .toList();
    }

    public void deleteById(String id) {
        store.remove(id);
    }
}
