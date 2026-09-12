package com.security.forecsic.service;

import com.security.forecsic.dto.PaperAuthorDto;
import com.security.forecsic.dto.PaperSubmitRequest;
import com.security.forecsic.dto.PaperSubmitResponse;
import com.security.forecsic.exception.ResourceNotFoundException;
import com.security.forecsic.model.AuditLog;
import com.security.forecsic.model.Register;
import com.security.forecsic.model.ResearchPaper;
import com.security.forecsic.model.ResearchPaperAuthor;
import com.security.forecsic.repositery.jpa.AuditLogRepository;
import com.security.forecsic.repositery.jpa.ResearchPaperAuthorRepository;
import com.security.forecsic.repositery.jpa.ResearchPaperRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResearchPaperService {

    private final ResearchPaperRepository researchPaperRepository;
    private final ResearchPaperAuthorRepository authorRepository;
    private final AuditLogRepository auditLogRepository;
    private final EmailService emailService;

    /**
     * Submit a new research paper (in DOC, DOCX, or PDF)
     */
    @Transactional
    public PaperSubmitResponse submitPaper(PaperSubmitRequest request, Register submittedBy) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Paper title cannot be blank.");
        }

        // Generate Unique Submission ID: e.g. FP-2026-1056
        String submissionId = generateUniqueSubmissionId();

        // Determine file type if url provided
        String fileType = request.getManuscriptFileType();
        if ((fileType == null || fileType.isBlank()) && request.getManuscriptFileUrl() != null) {
            fileType = FileStorageService.getFileExtension(request.getManuscriptFileUrl());
        }

        ResearchPaper paper = ResearchPaper.builder()
                .submissionId(submissionId)
                .title(request.getTitle().trim())
                .researchArea(request.getResearchArea())
                .abstractText(request.getAbstractText())
                .keywords(request.getKeywords())
                .status("SUBMITTED")
                .currentStage("Submission")
                .manuscriptFileUrl(request.getManuscriptFileUrl())
                .manuscriptFileType(fileType)
                .submittedBy(submittedBy)
                .build();

        ResearchPaper savedPaper = researchPaperRepository.save(paper);

        // Process Authors
        List<ResearchPaperAuthor> authorEntities = new ArrayList<>();
        String primaryAuthorEmail = (submittedBy != null) ? submittedBy.getEmail() : null;
        String primaryAuthorName = (submittedBy != null) ? submittedBy.getFullName() : null;

        if (request.getAuthors() != null && !request.getAuthors().isEmpty()) {
            for (int i = 0; i < request.getAuthors().size(); i++) {
                PaperAuthorDto authorDto = request.getAuthors().get(i);
                boolean isFirst = (i == 0) || authorDto.isFirstAuthor();

                ResearchPaperAuthor author = ResearchPaperAuthor.builder()
                        .researchPaper(savedPaper)
                        .name(authorDto.getName())
                        .email(authorDto.getEmail())
                        .university(authorDto.getUniversity())
                        .isFirstAuthor(isFirst)
                        .isCorrespondingAuthor(authorDto.isCorrespondingAuthor() || isFirst)
                        .authorOrder(authorDto.getAuthorOrder() != null ? authorDto.getAuthorOrder() : (i + 1))
                        .build();

                authorEntities.add(author);

                if (isFirst && authorDto.getEmail() != null && !authorDto.getEmail().isBlank()) {
                    primaryAuthorEmail = authorDto.getEmail();
                    primaryAuthorName = authorDto.getName();
                }
            }
            authorRepository.saveAll(authorEntities);
            savedPaper.setAuthors(authorEntities);
        } else if (submittedBy != null) {
            // Default first author from authenticated submitter
            ResearchPaperAuthor author = ResearchPaperAuthor.builder()
                    .researchPaper(savedPaper)
                    .name(submittedBy.getFullName())
                    .email(submittedBy.getEmail())
                    .university(submittedBy.getOrganization())
                    .isFirstAuthor(true)
                    .isCorrespondingAuthor(true)
                    .authorOrder(1)
                    .build();
            authorRepository.save(author);
            savedPaper.setAuthors(List.of(author));
        }

        // Audit Trail
        AuditLog auditLog = AuditLog.builder()
                .researchPaper(savedPaper)
                .actor(submittedBy)
                .action("SUBMIT_PAPER")
                .fromStatus(null)
                .toStatus("SUBMITTED")
                .notes("New research paper submitted with ID " + submissionId)
                .build();
        auditLogRepository.save(auditLog);

        // Send submission confirmation email via Resend
        if (primaryAuthorEmail != null && !primaryAuthorEmail.isBlank()) {
            emailService.sendPaperSubmissionConfirmationEmail(
                    primaryAuthorEmail,
                    primaryAuthorName,
                    submissionId,
                    savedPaper.getTitle(),
                    savedPaper.getResearchArea()
            );
        }

        log.info("Research paper successfully submitted. Submission ID: {}", submissionId);

        return PaperSubmitResponse.builder()
                .success(true)
                .message("Research paper submitted successfully. Confirmation email sent.")
                .submissionId(submissionId)
                .paperId(savedPaper.getId())
                .title(savedPaper.getTitle())
                .status(savedPaper.getStatus())
                .emailSentTo(primaryAuthorEmail)
                .build();
    }

    /**
     * Track paper by Submission ID
     */
    @Transactional(readOnly = true)
    public ResearchPaper getPaperBySubmissionId(String submissionId) {
        return researchPaperRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Research paper with submission ID '" + submissionId + "' not found."));
    }

    /**
     * Get paper by UUID
     */
    @Transactional(readOnly = true)
    public ResearchPaper getPaperById(UUID id) {
        return researchPaperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research paper not found with ID: " + id));
    }

    /**
     * Get all papers with optional status, search, and pagination
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAllPapers(String status, String search, int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<ResearchPaper> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), term);
                Predicate subIdLike = cb.like(cb.lower(root.get("submissionId")), term);
                Predicate areaLike = cb.like(cb.lower(root.get("researchArea")), term);
                predicates.add(cb.or(titleLike, subIdLike, areaLike));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ResearchPaper> paperPage = researchPaperRepository.findAll(spec, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", paperPage.getContent());
        response.put("total", paperPage.getTotalElements());
        response.put("totalPages", paperPage.getTotalPages());
        response.put("currentPage", page);
        return response;
    }

    /**
     * Get papers submitted by the authenticated user
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserSubmissions(Integer userId, int page, int limit) {
        if (userId == null) {
            throw new IllegalArgumentException("User identifier is required.");
        }

        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ResearchPaper> paperPage = researchPaperRepository.findBySubmittedById(userId, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", paperPage.getContent());
        response.put("total", paperPage.getTotalElements());
        response.put("totalPages", paperPage.getTotalPages());
        response.put("currentPage", page);
        return response;
    }

    private String generateUniqueSubmissionId() {
        Random random = new Random();
        int currentYear = Year.now().getValue();
        for (int attempts = 0; attempts < 10; attempts++) {
            int seq = 1000 + random.nextInt(9000);
            String candidateId = String.format("FP-%d-%d", currentYear, seq);
            if (researchPaperRepository.findBySubmissionId(candidateId).isEmpty()) {
                return candidateId;
            }
        }
        return "FP-" + currentYear + "-" + System.currentTimeMillis() % 100000;
    }
}
