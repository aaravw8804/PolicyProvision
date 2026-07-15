package com.telusko.policyprovision.dto.response;

import com.telusko.policyprovision.model.Proposal;
import com.telusko.policyprovision.model.ProposalStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Outbound representation of a Proposal.
 */
public record ProposalResponse(
        String id,
        String customerId,
        Integer policyTermYears,
        BigDecimal sumAssured,
        BigDecimal annualPremium,
        String paymentFrequency,
        String nomineeName,
        String nomineeRelationship,
        ProposalStatus status,
        String policyNumber,
        Instant createdAt,
        Instant submittedAt
) {
    public static ProposalResponse from(Proposal proposal) {
        return new ProposalResponse(
                proposal.getId(),
                proposal.getCustomerId(),
                proposal.getPolicyTermYears(),
                proposal.getSumAssured(),
                proposal.getAnnualPremium(),
                proposal.getPaymentFrequency(),
                proposal.getNomineeName(),
                proposal.getNomineeRelationship(),
                proposal.getStatus(),
                proposal.getPolicyNumber(),
                proposal.getCreatedAt(),
                proposal.getSubmittedAt()
        );
    }
}
