package com.telusko.policyprovision.dto.response;

import com.telusko.policyprovision.model.Customer;
import com.telusko.policyprovision.util.PanMasker;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;

/**
 * Outbound representation of a Customer. Kept separate from the domain
 * model so the API contract doesn't leak internal fields, and so the PAN
 * can be masked (bonus: masked PII in customer responses).
 */
public record CustomerResponse(
        String id,
        String fullName,
        LocalDate dateOfBirth,
        int age,
        String email,
        String phone,
        String pan,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerResponse from(Customer customer) {
        int age = Period.between(customer.getDateOfBirth(), LocalDate.now()).getYears();
        return new CustomerResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getDateOfBirth(),
                age,
                customer.getEmail(),
                customer.getPhone(),
                PanMasker.mask(customer.getPan()),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
