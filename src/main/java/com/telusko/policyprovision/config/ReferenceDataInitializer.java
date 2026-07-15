package com.telusko.policyprovision.config;

import com.telusko.policyprovision.repository.ReferenceMasterRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the in-memory Reference Master with the two categories the business
 * rules depend on: valid policy terms and valid payment frequencies.
 * Runs once at application startup.
 */
@Component
public class ReferenceDataInitializer implements CommandLineRunner {

    private final ReferenceMasterRepository referenceMasterRepository;

    public ReferenceDataInitializer(ReferenceMasterRepository referenceMasterRepository) {
        this.referenceMasterRepository = referenceMasterRepository;
    }

    @Override
    public void run(String... args) {
        referenceMasterRepository.put("POLICY_TERM", List.of("10", "15", "20", "25", "30"));
        referenceMasterRepository.put("PAYMENT_FREQUENCY",
                List.of("MONTHLY", "QUARTERLY", "HALF_YEARLY", "ANNUALLY"));
    }
}
