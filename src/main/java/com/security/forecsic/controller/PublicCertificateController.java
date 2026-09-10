package com.security.forecsic.controller;

import com.security.forecsic.dto.CertificateVerificationResponse;
import com.security.forecsic.dto.IssueResponse;
import com.security.forecsic.dto.JournalResponse;
import com.security.forecsic.service.PublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicCertificateController {

    private final PublisherService publisherService;

    // 1. Verify Certificate Authenticity via QR Code Hash (Publicly Accessible)
    @GetMapping("/certificates/verify/{qrCode}")
    public ResponseEntity<CertificateVerificationResponse> verifyCertificate(@PathVariable String qrCode) {
        CertificateVerificationResponse response = publisherService.verifyCertificate(qrCode);
        return ResponseEntity.ok(response);
    }

    // 2. Public Catalog: Journals
    @GetMapping("/journals")
    public ResponseEntity<List<JournalResponse>> getPublicJournals() {
        return ResponseEntity.ok(publisherService.getJournals());
    }

    // 3. Public Catalog: Issues
    @GetMapping("/issues")
    public ResponseEntity<List<IssueResponse>> getPublicIssues(@RequestParam(required = false) UUID journalId) {
        return ResponseEntity.ok(publisherService.getIssues(journalId));
    }

    // 4. Public Catalog: Published Papers
    @GetMapping("/published-papers")
    public ResponseEntity<Map<String, Object>> getPublicPublishedPapers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) UUID journalId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(publisherService.getPublishedPapers(page, limit, journalId, year, search));
    }
}
