package com.security.forecsic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "researchpaper")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ResearchPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "submission_id", unique = true, nullable = false, length = 100)
    private String submissionId; // e.g. 'FP-2026-1056'

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "research_area", length = 150)
    private String researchArea;

    @Column(columnDefinition = "TEXT")
    private String abstractText;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "SUBMITTED"; // 'SUBMITTED', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED', 'PUBLISHED'

    @Column(name = "current_stage", length = 50)
    @Builder.Default
    private String currentStage = "Submission"; // 'Submission', 'Under Review', 'Accepted', 'Published'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private Journal journal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private JournalIssue issue;

    @Column(unique = true, length = 100)
    private String doi; // e.g. '10.5958/JFSR.2026.1002'

    @Column(name = "published_at")
    private LocalDate publishedAt;

    @Column(name = "manuscript_file_url", columnDefinition = "TEXT")
    private String manuscriptFileUrl;

    @Column(name = "manuscript_file_type", length = 20)
    private String manuscriptFileType; // 'pdf', 'docx', 'doc'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private Register submittedBy;

    @OneToMany(mappedBy = "researchPaper", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<ResearchPaperAuthor> authors = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
