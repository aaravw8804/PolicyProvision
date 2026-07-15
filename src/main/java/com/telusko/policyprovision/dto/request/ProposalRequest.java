package com.telusko.policyprovision.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Inbound payload for POST /proposals.
 * Bean Validation covers required-ness and coarse numeric bounds; the fine
 * grained business rules (sum assured range, premium minimum, policy term
 * membership, payment frequency membership, PAN requirement, nominee rule)
 * live in ProposalService since they depend on cross-field and reference
 * data lookups.
 */
public record ProposalRequest(

        @NotBlank(message = "customerId is required")
        String customerId,

        @NotNull(message = "policyTermYears is required")
        Integer policyTermYears,

        @NotNull(message = "sumAssured is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "sumAssured must be positive")
        BigDecimal sumAssured,

        @NotNull(message = "annualPremium is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "annualPremium must be positive")
        BigDecimal annualPremium,

        @NotBlank(message = "paymentFrequency is required")
        String paymentFrequency,

        @NotBlank(message = "nomineeName is required")
        String nomineeName,

        String nomineeRelationship
) {
}
