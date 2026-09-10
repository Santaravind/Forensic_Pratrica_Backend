package com.security.forecsic.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.security.forecsic.dto.FileUploadResponse;
import com.security.forecsic.dto.PaperSubmitRequest;
import com.security.forecsic.dto.PaperSubmitResponse;
import com.security.forecsic.model.Register;
import com.security.forecsic.model.ResearchPaper;
import com.security.forecsic.service.CustomUserDetails;
import com.security.forecsic.service.FileStorageService;
import com.security.forecsic.service.ResearchPaperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/research-papers")
@RequiredArgsConstructor
@Slf4j
public class ResearchPaperController {

    private final ResearchPaperService researchPaperService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // 1. Submit Paper via JSON
    @PostMapping("/submit")
    public ResponseEntity<PaperSubmitResponse> submitPaper(
            @Valid @RequestBody PaperSubmitRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Register submitter = (userDetails != null) ? userDetails.getUser() : null;
        PaperSubmitResponse response = researchPaperService.submitPaper(request, submitter);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. Submit Paper via Multipart Form Data (direct file upload + json metadata or form fields)
    @PostMapping(value = "/submit-with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PaperSubmitResponse> submitPaperWithFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "metadata", required = false) String metadataJson,
            @RequestParam(value = "title", required = false) String formTitle,
            @RequestParam(value = "researchArea", required = false) String formResearchArea,
            @RequestParam(value = "abstractText", required = false) String formAbstractText,
            @RequestParam(value = "keywords", required = false) String formKeywords,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            // Upload the DOC, DOCX, or PDF file
            FileUploadResponse uploadResult = fileStorageService.uploadFile(file, "research_papers");

            PaperSubmitRequest request;
            if (metadataJson != null && !metadataJson.isBlank()) {
                request = objectMapper.readValue(metadataJson, PaperSubmitRequest.class);
            } else {
                request = PaperSubmitRequest.builder()
                        .title(formTitle != null ? formTitle : file.getOriginalFilename())
                        .researchArea(formResearchArea)
                        .abstractText(formAbstractText)
                        .keywords(formKeywords)
                        .build();
            }

            if (request.getTitle() == null || request.getTitle().isBlank()) {
                if (formTitle != null && !formTitle.isBlank()) {
                    request.setTitle(formTitle);
                } else {
                    request.setTitle(file.getOriginalFilename());
                }
            }

            request.setManuscriptFileUrl(uploadResult.getFileUrl());
            request.setManuscriptFileType(uploadResult.getFileType());

            Register submitter = (userDetails != null) ? userDetails.getUser() : null;
            PaperSubmitResponse response = researchPaperService.submitPaper(request, submitter);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Paper submission failed", e);
            throw new RuntimeException("Submission failed: " + e.getMessage(), e);
        }
    }

    // 3. Upload Document (DOC, DOCX, PDF)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "research_papers") String folder
    ) {
        FileUploadResponse response = fileStorageService.uploadFile(file, folder);
        return ResponseEntity.ok(response);
    }

    // 4. Track Research Paper by Submission ID (Public)
    @GetMapping("/track/{submissionId}")
    public ResponseEntity<ResearchPaper> trackPaper(@PathVariable String submissionId) {
        ResearchPaper paper = researchPaperService.getPaperBySubmissionId(submissionId);
        return ResponseEntity.ok(paper);
    }

    // 5. Get Paper by UUID
    @GetMapping("/{id}")
    public ResponseEntity<ResearchPaper> getPaperById(@PathVariable UUID id) {
        ResearchPaper paper = researchPaperService.getPaperById(id);
        return ResponseEntity.ok(paper);
    }

    // 6. Get All Papers (with optional status, search, and pagination)
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPapers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(researchPaperService.getAllPapers(status, search, page, limit));
    }
}
