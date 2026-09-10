package com.security.forecsic.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
    name = "journal_issues",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"journal_id", "volume_no", "issue_no"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "journal_id", nullable = false)
    private Journal journal;

    @Column(name = "volume_no", nullable = false)
    private Integer volumeNo;

    @Column(name = "issue_no", nullable = false)
    private Integer issueNo;

    @Column(name = "issue_title", nullable = false, length = 200)
    private String issueTitle; // e.g. 'Vol. 10, Issue 2 May 2026'

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false, length = 20)
    private String month;

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private boolean isPublished = false;

    @Column(name = "published_date")
    private LocalDate publishedDate;

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
