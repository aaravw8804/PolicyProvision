package com.telusko.policyprovision.service;

import com.telusko.policyprovision.dto.request.ProposalRequest;
import com.telusko.policyprovision.exception.BusinessValidationException;
import com.telusko.policyprovision.exception.InvalidStateException;
import com.telusko.policyprovision.exception.ResourceNotFoundException;
import com.telusko.policyprovision.model.Customer;
import com.telusko.policyprovision.model.Proposal;
import com.telusko.policyprovision.model.ProposalStatus;
import com.telusko.policyprovision.repository.ProposalRepository;
import com.telusko.policyprovision.util.IdGenerator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Owns every business rule for Policy Proposals: sum assured range, minimum
 * premium, PAN requirement, nominee rule, and reference-master membership
 * for policy term / payment frequency. Rules are re-validated both at
 * creation time and again at submission time, since reference data or the
 * linked customer's PAN could have changed in between.
 */
@Service
public class ProposalService {

    static final BigDecimal MIN_SUM_ASSURED = new BigDecimal("100000");
    static final BigDecimal MAX_SUM_ASSURED = new BigDecimal("50000000"); // Rs. 5,00,00,000
    static final BigDecimal MIN_ANNUAL_PREMIUM = new BigDecimal("5000");
    static final BigDecimal PAN_MANDATORY_PREMIUM_THRESHOLD = new BigDecimal("50000");

    private static final String ID_PREFIX = "PROP";
    private static final String POLICY_NUMBER_PREFIX = "POL";

    private final ProposalRepository proposalRepository;
    private final CustomerService customerService;
    private final ReferenceMasterService referenceMasterService;
    private final AuditService auditService;
    private final IdGenerator idGenerator;

    public ProposalService(ProposalRepository proposalRepository,
                            CustomerService customerService,
                            ReferenceMasterService referenceMasterService,
                            AuditService auditService,
                            IdGenerator idGenerator) {
        this.proposalRepository = proposalRepository;
        this.customerService = customerService;
        this.referenceMasterService = referenceMasterService;
        this.auditService = auditService;
        this.idGenerator = idGenerator;
    }

    public Proposal createProposal(ProposalRequest request) {
        Customer customer = customerService.getCustomer(request.customerId());
        validateBusinessRules(request, customer);

        Proposal proposal = Proposal.builder()
                .id(idGenerator.next(ID_PREFIX))
                .customerId(customer.getId())
                .policyTermYears(request.policyTermYears())
                .sumAssured(request.sumAssured())
                .annualPremium(request.annualPremium())
                .paymentFrequency(request.paymentFrequency().toUpperCase())
                .nomineeName(request.nomineeName().trim())
                .nomineeRelationship(request.nomineeRelationship())
                .status(ProposalStatus.DRAFT)
                .createdAt(Instant.now())
                .build();

        return proposalRepository.save(proposal);
    }

    public Proposal getProposal(String id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found: " + id));
    }

    public List<Proposal> getAllProposals() {
        return proposalRepository.findAll();
    }

    public Proposal submitProposal(String id) {
        Proposal proposal = getProposal(id);

        if (proposal.getStatus() == ProposalStatus.SUBMITTED) {
            throw new InvalidStateException("Proposal " + id + " has already been submitted");
        }

        // Re-validate at submission time: reference data or the customer's
        // PAN may have changed since the proposal was drafted.
        Customer customer = customerService.getCustomer(proposal.getCustomerId());
        ProposalRequest asRequest = new ProposalRequest(
                proposal.getCustomerId(), proposal.getPolicyTermYears(), proposal.getSumAssured(),
                proposal.getAnnualPremium(), proposal.getPaymentFrequency(),
                proposal.getNomineeName(), proposal.getNomineeRelationship());
        validateBusinessRules(asRequest, customer);

        String policyNumber = idGenerator.next(POLICY_NUMBER_PREFIX);
        proposal.setStatus(ProposalStatus.SUBMITTED);
        proposal.setPolicyNumber(policyNumber);
        proposal.setSubmittedAt(Instant.now());
        proposalRepository.save(proposal);

        auditService.recordSubmission(proposal);

        return proposal;
    }

    public void deleteProposal(String id) {
        Proposal proposal = getProposal(id);
        if (proposal.getStatus() == ProposalStatus.SUBMITTED) {
            throw new InvalidStateException(
                    "Cannot delete proposal " + id + ": it has already been submitted and has an audit trail");
        }
        proposalRepository.deleteById(id);
    }

    private void validateBusinessRules(ProposalRequest request, Customer customer) {
        if (!referenceMasterService.isValid("POLICY_TERM", String.valueOf(request.policyTermYears()))) {
            throw new BusinessValidationException(
                    "Invalid policy term: " + request.policyTermYears() + " years");
        }

        if (request.sumAssured().compareTo(MIN_SUM_ASSURED) < 0
                || request.sumAssured().compareTo(MAX_SUM_ASSURED) > 0) {
            throw new BusinessValidationException(
                    "Sum assured must be between %s and %s".formatted(MIN_SUM_ASSURED, MAX_SUM_ASSURED));
        }

        if (request.annualPremium().compareTo(MIN_ANNUAL_PREMIUM) < 0) {
            throw new BusinessValidationException(
                    "Annual premium must be at least " + MIN_ANNUAL_PREMIUM);
        }

        if (request.annualPremium().compareTo(PAN_MANDATORY_PREMIUM_THRESHOLD) > 0
                && (customer.getPan() == null || customer.getPan().isBlank())) {
            throw new BusinessValidationException(
                    "PAN is mandatory for customer " + customer.getId()
                            + " when annual premium exceeds " + PAN_MANDATORY_PREMIUM_THRESHOLD);
        }

        if (request.nomineeName().trim().equalsIgnoreCase(customer.getFullName().trim())) {
            throw new BusinessValidationException("Nominee cannot be the same as the customer");
        }

        if (!referenceMasterService.isValid("PAYMENT_FREQUENCY", request.paymentFrequency().toUpperCase())) {
            throw new BusinessValidationException(
                    "Invalid payment frequency: " + request.paymentFrequency());
        }
    }
}
