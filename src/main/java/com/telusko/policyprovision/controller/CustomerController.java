package com.telusko.policyprovision.controller;

import com.telusko.policyprovision.dto.request.CustomerRequest;
import com.telusko.policyprovision.dto.response.CustomerResponse;
import com.telusko.policyprovision.dto.response.PagedResponse;
import com.telusko.policyprovision.model.Customer;
import com.telusko.policyprovision.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        Customer created = customerService.createCustomer(request);
        return ResponseEntity.created(URI.create("/customers/" + created.getId()))
                .body(CustomerResponse.from(created));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<CustomerResponse>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<CustomerResponse> all = customerService.getAllCustomers().stream()
                .map(CustomerResponse::from)
                .toList();
        return ResponseEntity.ok(PagedResponse.of(all, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable String id) {
        return ResponseEntity.ok(CustomerResponse.from(customerService.getCustomer(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable String id,
                                                             @Valid @RequestBody CustomerRequest request) {
        Customer updated = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(CustomerResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
