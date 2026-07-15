package com.telusko.policyprovision.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * In-memory domain representation of a Policy Proposal.
 * A Proposal starts life as DRAFT (created via POST /proposals) and moves to
 * SUBMITTED (via POST /proposals/{id}/submit), at which point a policyNumber
 * is generated and an AuditRecord is created.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proposal {

    private String id;
    private String customerId;
    private Integer policyTermYears;
    private BigDecimal sumAssured;
    private BigDecimal annualPremium;
    private String paymentFrequency;
    private String nomineeName;
    private String nomineeRelationship;
    private ProposalStatus status;
    private String policyNumber;
    private Instant createdAt;
    private Instant submittedAt;
}
