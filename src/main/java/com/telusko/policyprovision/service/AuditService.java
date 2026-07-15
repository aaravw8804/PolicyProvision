package com.telusko.policyprovision.service;

import com.telusko.policyprovision.model.AuditRecord;
import com.telusko.policyprovision.model.Proposal;
import com.telusko.policyprovision.repository.AuditRepository;
import com.telusko.policyprovision.util.IdGenerator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Creates and retrieves audit trail entries. Audit records are only ever
 * created as a side effect of ProposalService.submitProposal - there is
 * deliberately no public "create audit" API for clients.
 */
@Service
public class AuditService {

    private static final String ID_PREFIX = "AUD";

    private final AuditRepository auditRepository;
    private final IdGenerator idGenerator;

    public AuditService(AuditRepository auditRepository, IdGenerator idGenerator) {
        this.auditRepository = auditRepository;
        this.idGenerator = idGenerator;
    }

    public AuditRecord recordSubmission(Proposal proposal) {
        AuditRecord record = AuditRecord.builder()
                .id(idGenerator.next(ID_PREFIX))
                .proposalId(proposal.getId())
                .customerId(proposal.getCustomerId())
                .policyNumber(proposal.getPolicyNumber())
                .action("PROPOSAL_SUBMITTED")
                .details("Proposal " + proposal.getId() + " submitted; policy number "
                        + proposal.getPolicyNumber() + " generated")
                .timestamp(Instant.now())
                .build();
        return auditRepository.save(record);
    }

    public List<AuditRecord> getAllAudits() {
        return auditRepository.findAll();
    }

    public List<AuditRecord> getAuditsByProposalId(String proposalId) {
        return auditRepository.findByProposalId(proposalId);
    }
}
