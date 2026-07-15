package com.telusko.policyprovision.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * In-memory domain representation of a Customer.
 * Never persisted to a database - held only in ConcurrentHashMap via CustomerRepository.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    private String id;
    private String fullName;
    private LocalDate dateOfBirth;
    private String email;
    private String phone;
    private String pan;
    private Instant createdAt;
    private Instant updatedAt;
}
