package com.telusko.policyprovision.util;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe sequential ID generator.
 * Produces IDs in the form PREFIX-0001, PREFIX-0002, ...
 * A separate AtomicLong counter is kept per prefix so Customer, Proposal,
 * and Audit IDs each have their own independent, gap-free sequence.
 */
@Component
public class IdGenerator {

    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    public String next(String prefix) {
        long value = counters
                .computeIfAbsent(prefix, p -> new AtomicLong(0))
                .incrementAndGet();
        return "%s-%04d".formatted(prefix, value);
    }
}
