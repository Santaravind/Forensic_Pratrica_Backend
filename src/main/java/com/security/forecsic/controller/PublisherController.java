package com.security.forecsic.controller;

import com.security.forecsic.dto.*;
import com.security.forecsic.model.*;
import com.security.forecsic.service.CustomUserDetails;
import com.security.forecsic.service.FileStorageService;
import com.security.forecsic.service.PublisherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/publisher")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PUBLISHER', 'ADMIN')")
public class PublisherController {

    private final PublisherService publisherService;
    private final FileStorageService fileStorageService;

    // 1. Dashboard & Statistics
    @GetMapping("/stats")
    public ResponseEntity<PublisherStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(publisherService.getDashboardStats());
    }

    // 2. Ingestion & Accepted Queue
    @GetMapping("/queue")
    public ResponseEntity<Map<String, Object>> getAcceptedQueue(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(publisherService.getAcceptedQueue(page, limit, search));
    }

    // 3. Publish Paper Action
    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publishManuscript(
            @Valid @RequestBody PublishManuscriptRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Register actor = (userDetails != null) ? userDetails.getUser() : null;
        Publication publication = publisherService.publishManuscript(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Manuscript successfully published!",
                "data", publication
        ));
    }

    // 4. Recently Published Papers Feed
    @GetMapping("/published-papers")
    public ResponseEntity<Map<String, Object>> getPublishedPapers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) UUID journalId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(publisherService.getPublishedPapers(page, limit, journalId, year, search));
    }

    // 5. Journal Management
    @GetMapping("/journals")
    public ResponseEntity<List<JournalResponse>> getJournals() {
        return ResponseEntity.ok(publisherService.getJournals());
    }

    @PostMapping("/journals")
    public ResponseEntity<Map<String, Object>> createJournal(@Valid @RequestBody JournalCreateRequest request) {
        Journal journal = publisherService.createJournal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", journal));
    }

    @PutMapping("/journals/{id}")
    public ResponseEntity<Map<String, Object>> updateJournal(
            @PathVariable UUID id,
            @Valid @RequestBody JournalCreateRequest request
    ) {
        Journal journal = publisherService.updateJournal(id, request);
        return ResponseEntity.ok(Map.of("success", true, "data", journal));
    }

    // 6. Issue Management
    @GetMapping("/issues")
    public ResponseEntity<List<IssueResponse>> getIssues(@RequestParam(required = false) UUID journalId) {
        return ResponseEntity.ok(publisherService.getIssues(journalId));
    }

    @PostMapping("/issues")
    public ResponseEntity<Map<String, Object>> createIssue(@Valid @RequestBody IssueCreateRequest request) {
        JournalIssue issue = publisherService.createIssue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", issue));
    }

    @PatchMapping("/issues/{id}/publish")
    public ResponseEntity<Map<String, Object>> publishIssue(@PathVariable UUID id) {
        JournalIssue issue = publisherService.publishIssue(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Issue released and published live!", "data", issue));
    }

    // 7. Certificates Management
    @GetMapping("/certificates")
    public ResponseEntity<Map<String, Object>> getCertificates(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(publisherService.getCertificates(search, page, limit));
    }

    @PostMapping("/certificates/generate")
    public ResponseEntity<Map<String, Object>> generateCertificate(
            @Valid @RequestBody CertificateGenerateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Register actor = (userDetails != null) ? userDetails.getUser() : null;
        Certificate cert = publisherService.generateOnDemandCertificate(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", cert));
    }

    // 8. Announcements
    @GetMapping("/announcements")
    public ResponseEntity<List<AnnouncementResponse>> getAnnouncements() {
        return ResponseEntity.ok(publisherService.getAnnouncements());
    }

    @PostMapping("/announcements")
    public ResponseEntity<Map<String, Object>> createAnnouncement(
            @Valid @RequestBody AnnouncementRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Register creator = (userDetails != null) ? userDetails.getUser() : null;
        Announcement announcement = publisherService.createAnnouncement(request, creator);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", announcement));
    }

    // 9. Direct Admin/Publisher to Author Emailing (via Resend)
    @PostMapping({"/mail-author", "/send-author-email"})
    public ResponseEntity<ApiResponse> sendAuthorEmail(
            @Valid @RequestBody SendAuthorEmailRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Register sender = (userDetails != null) ? userDetails.getUser() : null;
        publisherService.sendAuthorDirectEmail(request, sender);
        return ResponseEntity.ok(new ApiResponse(true, "Email successfully dispatched to author: " + request.getRecipientEmail()));
    }

    // 10. File Upload (DOC, DOCX, PDF) to Cloudinary
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "published_papers") String folder
    ) {
        FileUploadResponse response = fileStorageService.uploadFile(file, folder);
        return ResponseEntity.ok(response);
    }

    // 11. Seed Initial Demo Data (for testing)
    @PostMapping("/seed-demo")
    public ResponseEntity<Map<String, Object>> seedDemoData(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Register actor = (userDetails != null) ? userDetails.getUser() : null;
        return ResponseEntity.ok(publisherService.seedDemoData(actor));
    }
}
