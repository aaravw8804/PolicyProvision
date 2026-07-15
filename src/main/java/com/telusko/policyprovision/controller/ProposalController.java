package com.telusko.policyprovision.controller;

import com.telusko.policyprovision.dto.request.ProposalRequest;
import com.telusko.policyprovision.dto.response.ProposalResponse;
import com.telusko.policyprovision.model.Proposal;
import com.telusko.policyprovision.service.ProposalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/proposals")
public class ProposalController {

    private final ProposalService proposalService;

    public ProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @PostMapping
    public ResponseEntity<ProposalResponse> createProposal(@Valid @RequestBody ProposalRequest request) {
        Proposal created = proposalService.createProposal(request);
        return ResponseEntity.created(URI.create("/proposals/" + created.getId()))
                .body(ProposalResponse.from(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProposalResponse> getProposal(@PathVariable String id) {
        return ResponseEntity.ok(ProposalResponse.from(proposalService.getProposal(id)));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ProposalResponse> submitProposal(@PathVariable String id) {
        Proposal submitted = proposalService.submitProposal(id);
        return ResponseEntity.ok(ProposalResponse.from(submitted));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProposal(@PathVariable String id) {
        proposalService.deleteProposal(id);
        return ResponseEntity.noContent().build();
    }
}
