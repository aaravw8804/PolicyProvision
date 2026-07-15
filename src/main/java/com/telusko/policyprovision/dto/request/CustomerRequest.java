package com.telusko.policyprovision.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Inbound payload for POST /customers and PUT /customers/{id}.
 * Bean Validation here covers structural correctness (required fields,
 * formats). Business rules (age 18-65) are re-checked in CustomerService.
 */
public record CustomerRequest(

        @NotBlank(message = "fullName is required")
        String fullName,

        @NotNull(message = "dateOfBirth is required")
        @Past(message = "dateOfBirth must be in the past")
        LocalDate dateOfBirth,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        String email,

        @NotBlank(message = "phone is required")
        @Pattern(regexp = "\\d{10}", message = "phone must be a 10-digit number")
        String phone,

        @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]", message = "pan must match the format AAAAA9999A")
        String pan
) {
}
