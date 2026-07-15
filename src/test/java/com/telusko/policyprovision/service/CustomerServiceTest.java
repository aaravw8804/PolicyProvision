package com.telusko.policyprovision.service;

import com.telusko.policyprovision.dto.request.CustomerRequest;
import com.telusko.policyprovision.exception.BusinessValidationException;
import com.telusko.policyprovision.exception.InvalidStateException;
import com.telusko.policyprovision.exception.ResourceNotFoundException;
import com.telusko.policyprovision.model.Customer;
import com.telusko.policyprovision.model.Proposal;
import com.telusko.policyprovision.model.ProposalStatus;
import com.telusko.policyprovision.repository.CustomerRepository;
import com.telusko.policyprovision.repository.ProposalRepository;
import com.telusko.policyprovision.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain unit tests (no Spring context) against CustomerService, covering
 * the age-eligibility business rule and the customer lifecycle.
 */
class CustomerServiceTest {

    private CustomerRepository customerRepository;
    private ProposalRepository proposalRepository;
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerRepository = new CustomerRepository();
        proposalRepository = new ProposalRepository();
        customerService = new CustomerService(customerRepository, proposalRepository, new IdGenerator());
    }

    private CustomerRequest validRequest(LocalDate dob) {
        return new CustomerRequest("Aarav Sharma", dob, "aarav@example.com", "9876543210", "ABCDE1234F");
    }

    @Test
    void createCustomer_withValidAge_succeeds() {
        Customer created = customerService.createCustomer(validRequest(LocalDate.now().minusYears(30)));

        assertNotNull(created.getId());
        assertTrue(created.getId().startsWith("CUST-"));
        assertEquals("Aarav Sharma", created.getFullName());
    }

    @Test
    void createCustomer_belowMinimumAge_throwsBusinessValidationException() {
        CustomerRequest request = validRequest(LocalDate.now().minusYears(17));

        assertThrows(BusinessValidationException.class, () -> customerService.createCustomer(request));
    }

    @Test
    void createCustomer_aboveMaximumAge_throwsBusinessValidationException() {
        CustomerRequest request = validRequest(LocalDate.now().minusYears(66));

        assertThrows(BusinessValidationException.class, () -> customerService.createCustomer(request));
    }

    @Test
    void createCustomer_atExactBoundaryAges_succeeds() {
        assertDoesNotThrow(() -> customerService.createCustomer(validRequest(LocalDate.now().minusYears(18))));
        assertDoesNotThrow(() -> customerService.createCustomer(validRequest(LocalDate.now().minusYears(65))));
    }

    @Test
    void getCustomer_unknownId_throwsResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class, () -> customerService.getCustomer("CUST-9999"));
    }

    @Test
    void updateCustomer_revalidatesAge() {
        Customer created = customerService.createCustomer(validRequest(LocalDate.now().minusYears(30)));
        CustomerRequest badUpdate = validRequest(LocalDate.now().minusYears(10));

        assertThrows(BusinessValidationException.class,
                () -> customerService.updateCustomer(created.getId(), badUpdate));
    }

    @Test
    void deleteCustomer_withNoProposals_succeeds() {
        Customer created = customerService.createCustomer(validRequest(LocalDate.now().minusYears(30)));

        assertDoesNotThrow(() -> customerService.deleteCustomer(created.getId()));
        assertThrows(ResourceNotFoundException.class, () -> customerService.getCustomer(created.getId()));
    }

    @Test
    void deleteCustomer_withExistingProposals_throwsInvalidStateException() {
        Customer created = customerService.createCustomer(validRequest(LocalDate.now().minusYears(30)));
        proposalRepository.save(Proposal.builder()
                .id("PROP-0001")
                .customerId(created.getId())
                .status(ProposalStatus.DRAFT)
                .createdAt(Instant.now())
                .build());

        assertThrows(InvalidStateException.class, () -> customerService.deleteCustomer(created.getId()));
    }
}
