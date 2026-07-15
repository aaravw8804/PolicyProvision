package com.telusko.policyprovision.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * In-memory audit trail entry. Created automatically as a side effect of
 * proposal submission - never created directly by a client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditRecord {

    private String id;
    private String proposalId;
    private String customerId;
    private String policyNumber;
    private String action;
    private String details;
    private Instant timestamp;
}
