package com.telusko.policyprovision.dto.response;

import com.telusko.policyprovision.model.AuditRecord;

import java.time.Instant;

/**
 * Outbound representation of an AuditRecord.
 */
public record AuditResponse(
        String id,
        String proposalId,
        String customerId,
        String policyNumber,
        String action,
        String details,
        Instant timestamp
) {
    public static AuditResponse from(AuditRecord record) {
        return new AuditResponse(
                record.getId(),
                record.getProposalId(),
                record.getCustomerId(),
                record.getPolicyNumber(),
                record.getAction(),
                record.getDetails(),
                record.getTimestamp()
        );
    }
}
