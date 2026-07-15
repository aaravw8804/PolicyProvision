package com.telusko.policyprovision.service;

import com.telusko.policyprovision.dto.request.CustomerRequest;
import com.telusko.policyprovision.exception.BusinessValidationException;
import com.telusko.policyprovision.exception.InvalidStateException;
import com.telusko.policyprovision.exception.ResourceNotFoundException;
import com.telusko.policyprovision.model.Customer;
import com.telusko.policyprovision.repository.CustomerRepository;
import com.telusko.policyprovision.repository.ProposalRepository;
import com.telusko.policyprovision.util.IdGenerator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class CustomerService {

    static final int MIN_AGE = 18;
    static final int MAX_AGE = 65;
    private static final String ID_PREFIX = "CUST";

    private final CustomerRepository customerRepository;
    private final ProposalRepository proposalRepository;
    private final IdGenerator idGenerator;

    public CustomerService(CustomerRepository customerRepository,
                            ProposalRepository proposalRepository,
                            IdGenerator idGenerator) {
        this.customerRepository = customerRepository;
        this.proposalRepository = proposalRepository;
        this.idGenerator = idGenerator;
    }

    public Customer createCustomer(CustomerRequest request) {
        validateAge(request.dateOfBirth());

        Customer customer = Customer.builder()
                .id(idGenerator.next(ID_PREFIX))
                .fullName(request.fullName().trim())
                .dateOfBirth(request.dateOfBirth())
                .email(request.email().trim())
                .phone(request.phone().trim())
                .pan(request.pan())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return customerRepository.save(customer);
    }

    public Customer getCustomer(String id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer updateCustomer(String id, CustomerRequest request) {
        Customer existing = getCustomer(id);
        validateAge(request.dateOfBirth());

        existing.setFullName(request.fullName().trim());
        existing.setDateOfBirth(request.dateOfBirth());
        existing.setEmail(request.email().trim());
        existing.setPhone(request.phone().trim());
        existing.setPan(request.pan());
        existing.setUpdatedAt(Instant.now());

        return customerRepository.save(existing);
    }

    public void deleteCustomer(String id) {
        getCustomer(id); // ensures existence, throws 404 if not found
        boolean hasProposals = !proposalRepository.findByCustomerId(id).isEmpty();
        if (hasProposals) {
            throw new InvalidStateException(
                    "Cannot delete customer " + id + ": one or more proposals reference this customer");
        }
        customerRepository.deleteById(id);
    }

    private void validateAge(LocalDate dateOfBirth) {
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new BusinessValidationException(
                    "Customer age must be between %d and %d years (calculated age: %d)"
                            .formatted(MIN_AGE, MAX_AGE, age));
        }
    }
}
