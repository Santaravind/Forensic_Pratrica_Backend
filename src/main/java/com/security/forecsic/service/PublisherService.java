package com.security.forecsic.service;

import com.security.forecsic.dto.*;
import com.security.forecsic.exception.ResourceNotFoundException;
import com.security.forecsic.model.*;
import com.security.forecsic.repositery.jpa.*;
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

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublisherService {

    private final ResearchPaperRepository researchPaperRepository;
    private final ResearchPaperAuthorRepository authorRepository;
    private final JournalRepository journalRepository;
    private final JournalIssueRepository issueRepository;
    private final PublicationRepository publicationRepository;
    private final CertificateRepository certificateRepository;
    private final AnnouncementRepository announcementRepository;
    private final AuditLogRepository auditLogRepository;
    private final RegisterRepository registerRepository;
    private final CertificateGeneratorService certificateGeneratorService;
    private final EmailService emailService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /**
     * 1. Get Publisher Dashboard Statistics
     */
    @Transactional(readOnly = true)
    public PublisherStatsResponse getDashboardStats() {
        long journalsCount = journalRepository.countByIsActiveTrue();
        long issuesCount = issueRepository.countByIsPublishedTrue();
        long publishedCount = researchPaperRepository.countByStatus("PUBLISHED");
        long underReviewCount = researchPaperRepository.countByStatus("UNDER_REVIEW");
        long acceptedCount = researchPaperRepository.countByStatus("ACCEPTED");
        long rejectedCount = researchPaperRepository.countByStatus("REJECTED");
        long submittedCount = researchPaperRepository.countByStatus("SUBMITTED");
        long totalUsers = registerRepository.count();
        long totalDownloads = publicationRepository.sumDownloads();

        long totalPapers = publishedCount + underReviewCount + acceptedCount + rejectedCount + submittedCount;
        if (totalPapers == 0) totalPapers = 1;

        int acceptanceRate = (int) Math.round(((double) (acceptedCount + publishedCount) / totalPapers) * 100);
        int rejectionRate = (int) Math.round(((double) rejectedCount / totalPapers) * 100);

        List<PublisherStatsResponse.PieChartItem> pieChart = List.of(
                new PublisherStatsResponse.PieChartItem("Published", publishedCount, "#6366F1"),
                new PublisherStatsResponse.PieChartItem("Under Review", underReviewCount, "#3B82F6"),
                new PublisherStatsResponse.PieChartItem("Accepted", acceptedCount, "#10B981"),
                new PublisherStatsResponse.PieChartItem("Rejected", rejectedCount, "#F59E0B")
        );

        PublisherStatsResponse.PerformanceMetrics metrics = PublisherStatsResponse.PerformanceMetrics.builder()
                .acceptanceRate(acceptanceRate)
                .rejectionRate(rejectionRate)
                .avgReviewDays(18)
                .avgPublishDays(25)
                .build();

        PublisherStatsResponse.StatsData statsData = PublisherStatsResponse.StatsData.builder()
                .journalsPublished(journalsCount)
                .issuesPublished(issuesCount)
                .papersPublished(publishedCount)
                .registeredUsers(totalUsers)
                .totalDownloads(totalDownloads)
                .pieChart(pieChart)
                .metrics(metrics)
                .build();

        return PublisherStatsResponse.builder()
                .success(true)
                .data(statsData)
                .build();
    }

    /**
     * 2. Get Accepted Queue for Ingestion
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAcceptedQueue(int page, int limit, String search) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit), Sort.by(Sort.Direction.ASC, "updatedAt"));

        Specification<ResearchPaper> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), "ACCEPTED"));

            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), term);
                Predicate subIdLike = cb.like(cb.lower(root.get("submissionId")), term);
                Predicate areaLike = cb.like(cb.lower(root.get("researchArea")), term);
                predicates.add(cb.or(titleLike, subIdLike, areaLike));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ResearchPaper> paperPage = researchPaperRepository.findAll(spec, pageable);

        List<PublisherQueueItemResponse> items = paperPage.getContent().stream().map(this::mapToQueueItem).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", items);
        response.put("total", paperPage.getTotalElements());
        response.put("totalPages", paperPage.getTotalPages());
        response.put("currentPage", page);
        return response;
    }

    /**
     * 3. Publish Research Paper (Core Action)
     */
    @Transactional
    public Publication publishManuscript(PublishManuscriptRequest request, Register publishedBy) {
        ResearchPaper paper = researchPaperRepository.findById(request.getManuscriptId())
                .orElseThrow(() -> new ResourceNotFoundException("Research paper not found with ID: " + request.getManuscriptId()));

        if (!"ACCEPTED".equalsIgnoreCase(paper.getStatus()) && !"PUBLISHED".equalsIgnoreCase(paper.getStatus())) {
            throw new IllegalArgumentException("Paper status is '" + paper.getStatus() + "'. Only ACCEPTED papers can be published.");
        }

        Journal journal = journalRepository.findById(request.getJournalId())
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found with ID: " + request.getJournalId()));

        JournalIssue issue = issueRepository.findById(request.getIssueId())
                .orElseThrow(() -> new ResourceNotFoundException("Journal issue not found with ID: " + request.getIssueId()));

        // Verify DOI Uniqueness
        publicationRepository.findByDoi(request.getDoi()).ifPresent(existing -> {
            if (!existing.getResearchPaper().getId().equals(paper.getId())) {
                throw new IllegalArgumentException("DOI '" + request.getDoi() + "' is already assigned to another publication.");
            }
        });

        LocalDate pubDate = request.getPublishedDate() != null ? request.getPublishedDate() : LocalDate.now();

        // 1. Create or Update Publication
        Publication publication = publicationRepository.findByResearchPaperId(paper.getId())
                .orElse(Publication.builder().researchPaper(paper).build());

        publication.setJournal(journal);
        publication.setIssue(issue);
        publication.setDoi(request.getDoi().trim());
        publication.setPublishedDate(pubDate);
        publication.setStartPage(request.getStartPage());
        publication.setEndPage(request.getEndPage());
        publication.setFinalPdfUrl(request.getFinalPdfUrl().trim());
        publication.setPublishedBy(publishedBy);

        Publication savedPub = publicationRepository.save(publication);

        // 2. Update ResearchPaper status
        paper.setStatus("PUBLISHED");
        paper.setCurrentStage("Published");
        paper.setJournal(journal);
        paper.setIssue(issue);
        paper.setDoi(request.getDoi().trim());
        paper.setPublishedAt(pubDate);
        researchPaperRepository.save(paper);

        // 3. Record Audit Log
        AuditLog auditLog = AuditLog.builder()
                .researchPaper(paper)
                .actor(publishedBy)
                .action("PUBLISH_MANUSCRIPT")
                .fromStatus("ACCEPTED")
                .toStatus("PUBLISHED")
                .notes("Assigned DOI: " + request.getDoi() + " | Journal: " + journal.getCode() + " | Issue: " + issue.getIssueTitle())
                .build();
        auditLogRepository.save(auditLog);

        // 4. Generate Certificates if requested
        String firstCertificateUrl = null;
        if (request.isGenerateCertificates()) {
            try {
                List<Certificate> certs = certificateGeneratorService.generatePublicationCertificates(paper, publishedBy);
                if (!certs.isEmpty()) {
                    firstCertificateUrl = certs.get(0).getCertificatePdfUrl();
                }
            } catch (Exception e) {
                log.error("Error generating certificates for paper {}", paper.getId(), e);
            }
        }

        // 5. Send Notification Email via Resend if requested
        if (request.isSendNotificationEmail()) {
            String authorEmail = null;
            String authorName = null;

            if (paper.getAuthors() != null && !paper.getAuthors().isEmpty()) {
                ResearchPaperAuthor firstAuthor = paper.getAuthors().stream()
                        .filter(ResearchPaperAuthor::isFirstAuthor)
                        .findFirst()
                        .orElse(paper.getAuthors().get(0));
                authorEmail = firstAuthor.getEmail();
                authorName = firstAuthor.getName();
            } else if (paper.getSubmittedBy() != null) {
                authorEmail = paper.getSubmittedBy().getEmail();
                authorName = paper.getSubmittedBy().getFullName();
            }

            if (authorEmail != null && !authorEmail.isBlank()) {
                try {
                    emailService.sendPublicationSuccessEmail(
                            authorEmail,
                            authorName,
                            paper.getTitle(),
                            journal.getTitle(),
                            request.getDoi(),
                            firstCertificateUrl,
                            request.getFinalPdfUrl()
                    );
                } catch (Exception e) {
                    log.error("Error sending publication email to {}", authorEmail, e);
                }
            }
        }

        log.info("Manuscript {} published successfully under DOI: {}", paper.getId(), request.getDoi());
        return savedPub;
    }

    /**
     * 4. Get Recently Published Papers Feed
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPublishedPapers(int page, int limit, UUID journalId, Integer year, String search) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page - 1),
                Math.max(1, limit),
                Sort.by(Sort.Order.desc("publishedDate"), Sort.Order.desc("createdAt"))
        );

        Specification<Publication> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (journalId != null) {
                predicates.add(cb.equal(root.get("journal").get("id"), journalId));
            }

            if (year != null) {
                predicates.add(cb.equal(root.get("issue").get("year"), year));
            }

            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("researchPaper").get("title")), term);
                Predicate doiLike = cb.like(cb.lower(root.get("doi")), term);
                predicates.add(cb.or(titleLike, doiLike));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Publication> pubPage = publicationRepository.findAll(spec, pageable);

        List<PublishedPaperResponse> items = pubPage.getContent().stream().map(pub -> {
            ResearchPaper paper = pub.getResearchPaper();
            String authorName = "Primary Author";
            if (paper.getAuthors() != null && !paper.getAuthors().isEmpty()) {
                authorName = paper.getAuthors().stream()
                        .filter(ResearchPaperAuthor::isFirstAuthor)
                        .map(ResearchPaperAuthor::getName)
                        .findFirst()
                        .orElse(paper.getAuthors().get(0).getName());
            } else if (paper.getSubmittedBy() != null) {
                authorName = paper.getSubmittedBy().getFullName();
            }

            return PublishedPaperResponse.builder()
                    .id(pub.getId())
                    .manuscriptId(paper.getId())
                    .submissionId(paper.getSubmissionId())
                    .title(paper.getTitle())
                    .journal(pub.getJournal().getTitle())
                    .author(authorName)
                    .date(pub.getPublishedDate() != null ? pub.getPublishedDate().format(DATE_FORMATTER) : "")
                    .issue(pub.getIssue().getIssueTitle())
                    .doi(pub.getDoi())
                    .pdfUrl(pub.getFinalPdfUrl())
                    .downloadsCount(pub.getDownloadsCount())
                    .viewsCount(pub.getViewsCount())
                    .build();
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", items);
        response.put("total", pubPage.getTotalElements());
        response.put("currentPage", page);
        return response;
    }

    /**
     * 5. Journal Management
     */
    @Transactional(readOnly = true)
    public List<JournalResponse> getJournals() {
        return journalRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().map(j -> {
            long issueCount = issueRepository.findByJournalIdOrderByYearDescVolumeNoDescIssueNoDesc(j.getId()).size();
            long paperCount = publicationRepository.countByJournalId(j.getId());
            return JournalResponse.builder()
                    .id(j.getId())
                    .title(j.getTitle())
                    .code(j.getCode())
                    .issnPrint(j.getIssnPrint())
                    .issnOnline(j.getIssnOnline())
                    .description(j.getDescription())
                    .aimsScope(j.getAimsScope())
                    .coverImageUrl(j.getCoverImageUrl())
                    .isActive(j.isActive())
                    .totalIssues(issueCount)
                    .totalPapers(paperCount)
                    .createdAt(j.getCreatedAt())
                    .updatedAt(j.getUpdatedAt())
                    .build();
        }).toList();
    }

    @Transactional
    public Journal createJournal(JournalCreateRequest request) {
        journalRepository.findByCode(request.getCode().trim().toUpperCase()).ifPresent(j -> {
            throw new IllegalArgumentException("Journal with code '" + request.getCode() + "' already exists.");
        });

        Journal journal = Journal.builder()
                .title(request.getTitle().trim())
                .code(request.getCode().trim().toUpperCase())
                .issnPrint(request.getIssnPrint())
                .issnOnline(request.getIssnOnline())
                .description(request.getDescription())
                .aimsScope(request.getAimsScope())
                .coverImageUrl(request.getCoverImageUrl())
                .isActive(true)
                .build();

        return journalRepository.save(journal);
    }

    @Transactional
    public Journal updateJournal(UUID id, JournalCreateRequest request) {
        Journal journal = journalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found with ID: " + id));

        journal.setTitle(request.getTitle().trim());
        journal.setCode(request.getCode().trim().toUpperCase());
        journal.setIssnPrint(request.getIssnPrint());
        journal.setIssnOnline(request.getIssnOnline());
        journal.setDescription(request.getDescription());
        journal.setAimsScope(request.getAimsScope());
        journal.setCoverImageUrl(request.getCoverImageUrl());

        return journalRepository.save(journal);
    }

    /**
     * 6. Issue Management
     */
    @Transactional(readOnly = true)
    public List<IssueResponse> getIssues(UUID journalId) {
        List<JournalIssue> issues;
        if (journalId != null) {
            issues = issueRepository.findByJournalIdOrderByYearDescVolumeNoDescIssueNoDesc(journalId);
        } else {
            issues = issueRepository.findAllByOrderByYearDescVolumeNoDescIssueNoDesc();
        }

        return issues.stream().map(issue -> {
            long paperCount = publicationRepository.countByIssueId(issue.getId());
            return IssueResponse.builder()
                    .id(issue.getId())
                    .journalId(issue.getJournal().getId())
                    .journalTitle(issue.getJournal().getTitle())
                    .volumeNo(issue.getVolumeNo())
                    .issueNo(issue.getIssueNo())
                    .issueTitle(issue.getIssueTitle())
                    .year(issue.getYear())
                    .month(issue.getMonth())
                    .coverImageUrl(issue.getCoverImageUrl())
                    .isPublished(issue.isPublished())
                    .publishedDate(issue.getPublishedDate())
                    .papersCount(paperCount)
                    .build();
        }).toList();
    }

    @Transactional
    public JournalIssue createIssue(IssueCreateRequest request) {
        Journal journal = journalRepository.findById(request.getJournalId())
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found with ID: " + request.getJournalId()));

        String title = (request.getIssueTitle() != null && !request.getIssueTitle().isBlank())
                ? request.getIssueTitle().trim()
                : String.format("Vol. %d, Issue %d (%s %d)", request.getVolumeNo(), request.getIssueNo(), request.getMonth(), request.getYear());

        JournalIssue issue = JournalIssue.builder()
                .journal(journal)
                .volumeNo(request.getVolumeNo())
                .issueNo(request.getIssueNo())
                .issueTitle(title)
                .year(request.getYear())
                .month(request.getMonth().trim())
                .coverImageUrl(request.getCoverImageUrl())
                .isPublished(false)
                .build();

        return issueRepository.save(issue);
    }

    @Transactional
    public JournalIssue publishIssue(UUID id) {
        JournalIssue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal issue not found with ID: " + id));

        issue.setPublished(true);
        issue.setPublishedDate(LocalDate.now());
        return issueRepository.save(issue);
    }

    /**
     * 7. Certificate Management & Verification
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCertificates(String search, int page, int limit) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page - 1),
                Math.max(1, limit),
                Sort.by(Sort.Order.desc("issueDate"), Sort.Order.desc("createdAt"))
        );

        Specification<Certificate> spec = (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }
            String term = "%" + search.trim().toLowerCase() + "%";
            Predicate certNoLike = cb.like(cb.lower(root.get("certificateNo")), term);
            Predicate nameLike = cb.like(cb.lower(root.get("recipientName")), term);
            Predicate emailLike = cb.like(cb.lower(root.get("recipientEmail")), term);
            Predicate paperTitleLike = cb.like(cb.lower(root.get("researchPaper").get("title")), term);

            return cb.or(certNoLike, nameLike, emailLike, paperTitleLike);
        };

        Page<Certificate> certPage = certificateRepository.findAll(spec, pageable);

        List<CertificateResponse> data = certPage.getContent().stream().map(c -> CertificateResponse.builder()
                .id(c.getId())
                .certificateNo(c.getCertificateNo())
                .recipientName(c.getRecipientName())
                .recipientEmail(c.getRecipientEmail())
                .manuscriptId(c.getResearchPaper() != null ? c.getResearchPaper().getId() : null)
                .paperTitle(c.getResearchPaper() != null ? c.getResearchPaper().getTitle() : "")
                .certificateType(c.getCertificateType())
                .issueDate(c.getIssueDate())
                .certificatePdfUrl(c.getCertificatePdfUrl())
                .qrVerificationCode(c.getQrVerificationCode())
                .isRevoked(c.isRevoked())
                .build()
        ).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        response.put("total", certPage.getTotalElements());
        response.put("currentPage", page);
        return response;
    }

    @Transactional
    public Certificate generateOnDemandCertificate(CertificateGenerateRequest request, Register actor) {
        ResearchPaper paper = researchPaperRepository.findById(request.getManuscriptId())
                .orElseThrow(() -> new ResourceNotFoundException("Research paper not found with ID: " + request.getManuscriptId()));

        String name = request.getRecipientName();
        String email = request.getRecipientEmail();

        if (name == null || name.isBlank()) {
            name = (paper.getSubmittedBy() != null) ? paper.getSubmittedBy().getFullName() : "Author";
        }
        if (email == null || email.isBlank()) {
            email = (paper.getSubmittedBy() != null) ? paper.getSubmittedBy().getEmail() : "author@forensicpatrika.com";
        }

        return certificateGeneratorService.generateSingleCertificate(
                paper,
                actor,
                name,
                email,
                request.getCertificateType() != null ? request.getCertificateType() : CertificateType.AUTHOR_PUBLICATION
        );
    }

    @Transactional(readOnly = true)
    public CertificateVerificationResponse verifyCertificate(String qrCode) {
        Optional<Certificate> certOpt = certificateRepository.findByQrVerificationCode(qrCode);
        if (certOpt.isEmpty()) {
            // Also check by certificate number
            certOpt = certificateRepository.findByCertificateNo(qrCode);
        }

        if (certOpt.isEmpty()) {
            return CertificateVerificationResponse.builder()
                    .valid(false)
                    .message("Certificate not found. The provided verification code is invalid.")
                    .build();
        }

        Certificate cert = certOpt.get();
        ResearchPaper paper = cert.getResearchPaper();
        String paperTitle = paper != null ? paper.getTitle() : "N/A";
        String journalTitle = (paper != null && paper.getJournal() != null) ? paper.getJournal().getTitle() : "Forensic Patrika Journal";
        String issueTitle = (paper != null && paper.getIssue() != null) ? paper.getIssue().getIssueTitle() : "N/A";
        String doi = paper != null ? paper.getDoi() : "N/A";

        if (cert.isRevoked()) {
            return CertificateVerificationResponse.builder()
                    .valid(false)
                    .message("Certificate has been REVOKED.")
                    .certificateNo(cert.getCertificateNo())
                    .recipientName(cert.getRecipientName())
                    .paperTitle(paperTitle)
                    .journalTitle(journalTitle)
                    .issueTitle(issueTitle)
                    .doi(doi)
                    .issueDate(cert.getIssueDate())
                    .certificateType(cert.getCertificateType().name())
                    .certificatePdfUrl(cert.getCertificatePdfUrl())
                    .isRevoked(true)
                    .build();
        }

        return CertificateVerificationResponse.builder()
                .valid(true)
                .message("Certificate is authentic and valid.")
                .certificateNo(cert.getCertificateNo())
                .recipientName(cert.getRecipientName())
                .paperTitle(paperTitle)
                .journalTitle(journalTitle)
                .issueTitle(issueTitle)
                .doi(doi)
                .issueDate(cert.getIssueDate())
                .certificateType(cert.getCertificateType().name())
                .certificatePdfUrl(cert.getCertificatePdfUrl())
                .isRevoked(false)
                .build();
    }

    /**
     * 8. Announcement Management
     */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getAnnouncements() {
        return announcementRepository.findByIsActiveTrueOrderByCreatedAtDesc().stream().map(a ->
                AnnouncementResponse.builder()
                        .id(a.getId())
                        .title(a.getTitle())
                        .content(a.getContent())
                        .targetRole(a.getTargetRole())
                        .isActive(a.isActive())
                        .createdByName(a.getCreatedBy() != null ? a.getCreatedBy().getFullName() : "Admin")
                        .expiresAt(a.getExpiresAt())
                        .createdAt(a.getCreatedAt())
                        .build()
        ).toList();
    }

    @Transactional
    public Announcement createAnnouncement(AnnouncementRequest request, Register creator) {
        Announcement announcement = Announcement.builder()
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .targetRole(request.getTargetRole() != null ? request.getTargetRole() : "ALL")
                .isActive(true)
                .createdBy(creator)
                .expiresAt(request.getExpiresAt())
                .build();

        return announcementRepository.save(announcement);
    }

    /**
     * 9. Send Direct Email from Admin/Publisher to Author
     */
    @Transactional
    public void sendAuthorDirectEmail(SendAuthorEmailRequest request, Register sender) {
        String paperTitle = request.getPaperTitle();
        ResearchPaper paper = null;

        if (request.getPaperId() != null) {
            paper = researchPaperRepository.findById(request.getPaperId()).orElse(null);
            if (paper != null && (paperTitle == null || paperTitle.isBlank())) {
                paperTitle = paper.getTitle();
            }
        }

        String senderName = (sender != null && sender.getFullName() != null) ? sender.getFullName() : "Editorial & Publishing Team";

        emailService.sendCustomAuthorEmail(
                request.getRecipientEmail().trim(),
                request.getRecipientName(),
                request.getSubject().trim(),
                request.getMessageContent().trim(),
                paperTitle,
                senderName
        );

        // Record Audit Log if associated with a paper
        if (paper != null) {
            AuditLog auditLog = AuditLog.builder()
                    .researchPaper(paper)
                    .actor(sender)
                    .action("SEND_AUTHOR_EMAIL")
                    .fromStatus(paper.getStatus())
                    .toStatus(paper.getStatus())
                    .notes("Direct email sent to " + request.getRecipientEmail() + ": " + request.getSubject())
                    .build();
            auditLogRepository.save(auditLog);
        }
    }

    /**
     * 10. Seed Demo Data (Journals, Issues, and Sample Accepted Papers in DOC, DOCX, and PDF)
     */
    @Transactional
    public Map<String, Object> seedDemoData(Register actor) {
        // 1. Journal
        Journal journal = journalRepository.findByCode("JFSR").orElseGet(() -> {
            Journal j = Journal.builder()
                    .title("Journal of Forensic Science and Research")
                    .code("JFSR")
                    .issnPrint("2456-1234")
                    .issnOnline("2456-5678")
                    .description("Premier peer-reviewed open access international journal covering all forensic science disciplines.")
                    .aimsScope("DNA profiling, Cyber Forensics, Toxicology, Ballistics, Fingerprint Analysis, Crime Scene Investigation.")
                    .coverImageUrl("https://images.unsplash.com/photo-1579154204601-01588f351e67?auto=format&fit=crop&q=80&w=800")
                    .isActive(true)
                    .build();
            return journalRepository.save(j);
        });

        // 2. Issue
        JournalIssue issue = issueRepository.findByJournalIdAndVolumeNoAndIssueNo(journal.getId(), 10, 2).orElseGet(() -> {
            JournalIssue ji = JournalIssue.builder()
                    .journal(journal)
                    .volumeNo(10)
                    .issueNo(2)
                    .issueTitle("Vol. 10, Issue 2 (May 2026)")
                    .year(2026)
                    .month("May")
                    .isPublished(true)
                    .publishedDate(LocalDate.of(2026, 5, 15))
                    .coverImageUrl("https://images.unsplash.com/photo-1532094349884-543bc11b234d?auto=format&fit=crop&q=80&w=800")
                    .build();
            return issueRepository.save(ji);
        });

        // 3. Accepted Papers in queue (DOC, DOCX, PDF)
        int seededCount = 0;
        if (researchPaperRepository.findBySubmissionId("FP-2026-1056").isEmpty()) {
            ResearchPaper p1 = ResearchPaper.builder()
                    .submissionId("FP-2026-1056")
                    .title("Advancements in Forensic DNA Analysis Using Next-Generation Sequencing Technologies")
                    .researchArea("Genetics")
                    .abstractText("This study presents empirical validation of NGS for highly degraded DNA samples in forensic criminalistics.")
                    .status("ACCEPTED")
                    .currentStage("Accepted")
                    .manuscriptFileUrl("https://storage.forensicpatrika.com/demo/FP-2026-1056.docx")
                    .manuscriptFileType("docx")
                    .submittedBy(actor)
                    .build();
            ResearchPaper savedP1 = researchPaperRepository.save(p1);

            ResearchPaperAuthor a1 = ResearchPaperAuthor.builder()
                    .researchPaper(savedP1)
                    .name("Dr. Indresh Kumar")
                    .email("indresh@nfsu.ac.in")
                    .university("National Forensic Sciences University")
                    .isFirstAuthor(true)
                    .isCorrespondingAuthor(true)
                    .authorOrder(1)
                    .build();
            authorRepository.save(a1);
            savedP1.setAuthors(List.of(a1));
            seededCount++;
        }

        if (researchPaperRepository.findBySubmissionId("FP-2026-1088").isEmpty()) {
            ResearchPaper p2 = ResearchPaper.builder()
                    .submissionId("FP-2026-1088")
                    .title("AI-Assisted Ballistic Toolmark Identification and Digital Striation Mapping")
                    .researchArea("Ballistics")
                    .abstractText("High-resolution optical profilometry combined with deep convolutional networks for ballistic matching.")
                    .status("ACCEPTED")
                    .currentStage("Accepted")
                    .manuscriptFileUrl("https://storage.forensicpatrika.com/demo/FP-2026-1088.pdf")
                    .manuscriptFileType("pdf")
                    .submittedBy(actor)
                    .build();
            ResearchPaper savedP2 = researchPaperRepository.save(p2);

            ResearchPaperAuthor a2 = ResearchPaperAuthor.builder()
                    .researchPaper(savedP2)
                    .name("Prof. Samantha Ray")
                    .email("samantha.ray@forensic-research.org")
                    .university("Central Forensic Science Laboratory")
                    .isFirstAuthor(true)
                    .isCorrespondingAuthor(true)
                    .authorOrder(1)
                    .build();
            authorRepository.save(a2);
            savedP2.setAuthors(List.of(a2));
            seededCount++;
        }

        if (researchPaperRepository.findBySubmissionId("FP-2026-1092").isEmpty()) {
            ResearchPaper p3 = ResearchPaper.builder()
                    .submissionId("FP-2026-1092")
                    .title("Chromatographic Profiling of Novel Synthetic Opioids in Forensic Toxicology")
                    .researchArea("Toxicology")
                    .abstractText("Comprehensive LC-MS/MS validation for ultra-trace detection of synthetic fentanyl analogues.")
                    .status("ACCEPTED")
                    .currentStage("Accepted")
                    .manuscriptFileUrl("https://storage.forensicpatrika.com/demo/FP-2026-1092.doc")
                    .manuscriptFileType("doc")
                    .submittedBy(actor)
                    .build();
            ResearchPaper savedP3 = researchPaperRepository.save(p3);

            ResearchPaperAuthor a3 = ResearchPaperAuthor.builder()
                    .researchPaper(savedP3)
                    .name("Dr. Rajesh Varma")
                    .email("r.varma@aiims.edu")
                    .university("AIIMS Department of Forensic Medicine")
                    .isFirstAuthor(true)
                    .isCorrespondingAuthor(true)
                    .authorOrder(1)
                    .build();
            authorRepository.save(a3);
            savedP3.setAuthors(List.of(a3));
            seededCount++;
        }

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Demo data verified and seeded successfully!");
        res.put("journalId", journal.getId());
        res.put("issueId", issue.getId());
        res.put("seededPapersCount", seededCount);
        return res;
    }

    private PublisherQueueItemResponse mapToQueueItem(ResearchPaper paper) {
        PaperAuthorDto firstAuthorDto = null;
        List<PaperAuthorDto> authorsDto = new ArrayList<>();

        if (paper.getAuthors() != null && !paper.getAuthors().isEmpty()) {
            for (ResearchPaperAuthor a : paper.getAuthors()) {
                PaperAuthorDto dto = PaperAuthorDto.builder()
                        .name(a.getName())
                        .email(a.getEmail())
                        .university(a.getUniversity())
                        .isFirstAuthor(a.isFirstAuthor())
                        .isCorrespondingAuthor(a.isCorrespondingAuthor())
                        .authorOrder(a.getAuthorOrder())
                        .build();
                authorsDto.add(dto);
                if (a.isFirstAuthor() && firstAuthorDto == null) {
                    firstAuthorDto = dto;
                }
            }
            if (firstAuthorDto == null && !authorsDto.isEmpty()) {
                firstAuthorDto = authorsDto.get(0);
            }
        } else if (paper.getSubmittedBy() != null) {
            firstAuthorDto = PaperAuthorDto.builder()
                    .name(paper.getSubmittedBy().getFullName())
                    .email(paper.getSubmittedBy().getEmail())
                    .university(paper.getSubmittedBy().getOrganization())
                    .isFirstAuthor(true)
                    .isCorrespondingAuthor(true)
                    .authorOrder(1)
                    .build();
            authorsDto.add(firstAuthorDto);
        }

        return PublisherQueueItemResponse.builder()
                .id(paper.getId())
                .submissionId(paper.getSubmissionId())
                .title(paper.getTitle())
                .researchArea(paper.getResearchArea())
                .abstractText(paper.getAbstractText())
                .status(paper.getStatus())
                .submittedAt(paper.getCreatedAt())
                .acceptedAt(paper.getUpdatedAt())
                .firstAuthor(firstAuthorDto)
                .authors(authorsDto)
                .manuscriptFileUrl(paper.getManuscriptFileUrl())
                .manuscriptFileType(paper.getManuscriptFileType())
                .build();
    }
}
