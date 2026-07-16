package com.telusko.policyprovision.controller;

import com.telusko.policyprovision.dto.request.ProposalRequest;
import com.telusko.policyprovision.dto.response.PagedResponse;
import com.telusko.policyprovision.dto.response.ProposalResponse;
import com.telusko.policyprovision.model.Proposal;
import com.telusko.policyprovision.service.ProposalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

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

    @GetMapping
    public ResponseEntity<PagedResponse<ProposalResponse>> getAllProposals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<ProposalResponse> all = proposalService.getAllProposals().stream()
                .map(ProposalResponse::from)
                .toList();
        return ResponseEntity.ok(PagedResponse.of(all, page, size));
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
