package com.telusko.policyprovision.service;

import com.telusko.policyprovision.dto.request.CustomerRequest;
import com.telusko.policyprovision.dto.request.ProposalRequest;
import com.telusko.policyprovision.exception.BusinessValidationException;
import com.telusko.policyprovision.exception.InvalidStateException;
import com.telusko.policyprovision.exception.ResourceNotFoundException;
import com.telusko.policyprovision.model.AuditRecord;
import com.telusko.policyprovision.model.Customer;
import com.telusko.policyprovision.model.Proposal;
import com.telusko.policyprovision.model.ProposalStatus;
import com.telusko.policyprovision.repository.CustomerRepository;
import com.telusko.policyprovision.repository.AuditRepository;
import com.telusko.policyprovision.repository.ProposalRepository;
import com.telusko.policyprovision.repository.ReferenceMasterRepository;
import com.telusko.policyprovision.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain unit tests (no Spring context) covering ProposalService business
 * rules and, critically, the full proposal submission flow: creation ->
 * submission -> policy number generation -> status update -> audit record
 * creation, as required by the assignment's "Expected Application Flow".
 */
class ProposalServiceTest {

    private CustomerService customerService;
    private ProposalService proposalService;
    private AuditService auditService;
    private ProposalRepository proposalRepository;

    private Customer customer;

    @BeforeEach
    void setUp() {
        CustomerRepository customerRepository = new CustomerRepository();
        proposalRepository = new ProposalRepository();
        AuditRepository auditRepository = new AuditRepository();
        ReferenceMasterRepository referenceMasterRepository = new ReferenceMasterRepository();
        referenceMasterRepository.put("POLICY_TERM", List.of("10", "15", "20", "25", "30"));
        referenceMasterRepository.put("PAYMENT_FREQUENCY",
                List.of("MONTHLY", "QUARTERLY", "HALF_YEARLY", "ANNUALLY"));
        IdGenerator idGenerator = new IdGenerator();

        customerService = new CustomerService(customerRepository, proposalRepository, idGenerator);
        ReferenceMasterService referenceMasterService = new ReferenceMasterService(referenceMasterRepository);
        auditService = new AuditService(auditRepository, idGenerator);
        proposalService = new ProposalService(
                proposalRepository, customerService, referenceMasterService, auditService, idGenerator);

        customer = customerService.createCustomer(new CustomerRequest(
                "Aarav Sharma", LocalDate.now().minusYears(30),
                "aarav@example.com", "9876543210", "ABCDE1234F"));
    }

    private ProposalRequest validRequest() {
        return new ProposalRequest(customer.getId(), 20, new BigDecimal("500000"),
                new BigDecimal("12000"), "MONTHLY", "Priya Sharma", "Spouse");
    }

    @Test
    void createProposal_withValidData_succeedsAsDraft() {
        Proposal proposal = proposalService.createProposal(validRequest());

        assertNotNull(proposal.getId());
        assertTrue(proposal.getId().startsWith("PROP-"));
        assertEquals(ProposalStatus.DRAFT, proposal.getStatus());
        assertNull(proposal.getPolicyNumber());
    }

    @Test
    void createProposal_unknownCustomer_throwsResourceNotFoundException() {
        ProposalRequest request = new ProposalRequest("CUST-9999", 20, new BigDecimal("500000"),
                new BigDecimal("12000"), "MONTHLY", "Priya Sharma", "Spouse");

        assertThrows(ResourceNotFoundException.class, () -> proposalService.createProposal(request));
    }

    @Test
    void createProposal_invalidPolicyTerm_throwsBusinessValidationException() {
        ProposalRequest request = new ProposalRequest(customer.getId(), 12, new BigDecimal("500000"),
                new BigDecimal("12000"), "MONTHLY", "Priya Sharma", "Spouse");

        assertThrows(BusinessValidationException.class, () -> proposalService.createProposal(request));
    }

    @Test
    void createProposal_sumAssuredBelowMinimum_throwsBusinessValidationException() {
        ProposalRequest request = new ProposalRequest(customer.getId(), 20, new BigDecimal("50000"),
                new BigDecimal("12000"), "MONTHLY", "Priya Sharma", "Spouse");

        assertThrows(BusinessValidationException.class, () -> proposalService.createProposal(request));
    }

    @Test
    void createProposal_sumAssuredAboveMaximum_throwsBusinessValidationException() {
        ProposalRequest request = new ProposalRequest(customer.getId(), 20, new BigDecimal("60000000"),
                new BigDecimal("12000"), "MONTHLY", "Priya Sharma", "Spouse");

        assertThrows(BusinessValidationException.class, () -> proposalService.createProposal(request));
    }

    @Test
    void createProposal_premiumBelowMinimum_throwsBusinessValidationException() {
        ProposalRequest request = new ProposalRequest(customer.getId(), 20, new BigDecimal("500000"),
                new BigDecimal("4000"), "MONTHLY", "Priya Sharma", "Spouse");

        assertThrows(BusinessValidationException.class, () -> proposalService.createProposal(request));
    }

    @Test
    void createProposal_premiumAboveThresholdWithoutPan_throwsBusinessValidationException() {
        Customer noPanCustomer = customerService.createCustomer(new CustomerRequest(
                "Rohan Verma", LocalDate.now().minusYears(28),
                "rohan@example.com", "9876500000", null));

        ProposalRequest request = new ProposalRequest(noPanCustomer.getId(), 20, new BigDecimal("500000"),
                new BigDecimal("60000"), "MONTHLY", "Meena Verma", "Spouse");

        assertThrows(BusinessValidationException.class, () -> proposalService.createProposal(request));
    }

    @Test
    void createProposal_premiumAboveThresholdWithPan_succeeds() {
        ProposalRequest request = new ProposalRequest(customer.getId(), 20, new BigDecimal("500000"),
                new BigDecimal("60000"), "MONTHLY", "Priya Sharma", "Spouse");

        assertDoesNotThrow(() -> proposalService.createProposal(request));
    }

    @Test
    void createProposal_nomineeSameAsCustomer_throwsBusinessValidationException() {
        ProposalRequest request = new ProposalRequest(customer.getId(), 20, new BigDecimal("500000"),
                new BigDecimal("12000"), "MONTHLY", "  aarav sharma  ", "Self");

        assertThrows(BusinessValidationException.class, () -> proposalService.createProposal(request));
    }

    @Test
    void createProposal_invalidPaymentFrequency_throwsBusinessValidationException() {
        ProposalRequest request = new ProposalRequest(customer.getId(), 20, new BigDecimal("500000"),
                new BigDecimal("12000"), "WEEKLY", "Priya Sharma", "Spouse");

        assertThrows(BusinessValidationException.class, () -> proposalService.createProposal(request));
    }

    @Test
    void submitProposal_fullFlow_generatesPolicyNumberUpdatesStatusAndCreatesAudit() {
        Proposal proposal = proposalService.createProposal(validRequest());

        Proposal submitted = proposalService.submitProposal(proposal.getId());

        assertEquals(ProposalStatus.SUBMITTED, submitted.getStatus());
        assertNotNull(submitted.getPolicyNumber());
        assertTrue(submitted.getPolicyNumber().startsWith("POL-"));
        assertNotNull(submitted.getSubmittedAt());

        List<AuditRecord> audits = auditService.getAuditsByProposalId(proposal.getId());
        assertEquals(1, audits.size());
        assertEquals("PROPOSAL_SUBMITTED", audits.get(0).getAction());
        assertEquals(submitted.getPolicyNumber(), audits.get(0).getPolicyNumber());
    }

    @Test
    void submitProposal_alreadySubmitted_throwsInvalidStateException() {
        Proposal proposal = proposalService.createProposal(validRequest());
        proposalService.submitProposal(proposal.getId());

        assertThrows(InvalidStateException.class, () -> proposalService.submitProposal(proposal.getId()));
    }

    @Test
    void submitProposal_unknownId_throwsResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class, () -> proposalService.submitProposal("PROP-9999"));
    }

    @Test
    void deleteProposal_inDraftStatus_succeeds() {
        Proposal proposal = proposalService.createProposal(validRequest());

        assertDoesNotThrow(() -> proposalService.deleteProposal(proposal.getId()));
        assertThrows(ResourceNotFoundException.class, () -> proposalService.getProposal(proposal.getId()));
    }

    @Test
    void deleteProposal_afterSubmission_throwsInvalidStateException() {
        Proposal proposal = proposalService.createProposal(validRequest());
        proposalService.submitProposal(proposal.getId());

        assertThrows(InvalidStateException.class, () -> proposalService.deleteProposal(proposal.getId()));
    }
}
