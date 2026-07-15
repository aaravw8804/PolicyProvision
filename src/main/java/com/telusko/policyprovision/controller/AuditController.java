package com.telusko.policyprovision.controller;

import com.telusko.policyprovision.dto.response.AuditResponse;
import com.telusko.policyprovision.dto.response.PagedResponse;
import com.telusko.policyprovision.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/audits")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<AuditResponse>> getAllAudits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<AuditResponse> all = auditService.getAllAudits().stream()
                .map(AuditResponse::from)
                .toList();
        return ResponseEntity.ok(PagedResponse.of(all, page, size));
    }

    // Bonus: audit lookup by entity ID (proposal ID)
    @GetMapping("/entity/{proposalId}")
    public ResponseEntity<List<AuditResponse>> getAuditsByProposalId(@PathVariable String proposalId) {
        List<AuditResponse> audits = auditService.getAuditsByProposalId(proposalId).stream()
                .map(AuditResponse::from)
                .toList();
        return ResponseEntity.ok(audits);
    }
}
