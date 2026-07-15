package com.telusko.policyprovision.service;

import com.telusko.policyprovision.exception.ResourceNotFoundException;
import com.telusko.policyprovision.repository.ReferenceMasterRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReferenceMasterService {

    private final ReferenceMasterRepository referenceMasterRepository;

    public ReferenceMasterService(ReferenceMasterRepository referenceMasterRepository) {
        this.referenceMasterRepository = referenceMasterRepository;
    }

    public List<String> getValues(String category) {
        if (!referenceMasterRepository.hasCategory(category)) {
            throw new ResourceNotFoundException("Unknown reference-master category: " + category);
        }
        return referenceMasterRepository.get(category);
    }

    public boolean isValid(String category, String value) {
        return referenceMasterRepository.isValidValue(category, value);
    }
}
