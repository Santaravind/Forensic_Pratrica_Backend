package com.security.forecsic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "certificate_no", unique = true, nullable = false, length = 100)
    private String certificateNo; // e.g. 'CERT-FP-2026-1002-01'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id")
    private Register recipientUser;

    @Column(name = "recipient_name", nullable = false, length = 200)
    private String recipientName;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manuscript_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ResearchPaper researchPaper;

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type", length = 50)
    @Builder.Default
    private CertificateType certificateType = CertificateType.AUTHOR_PUBLICATION;

    @Column(name = "issue_date", nullable = false)
    @Builder.Default
    private LocalDate issueDate = LocalDate.now();

    @Column(name = "certificate_pdf_url", columnDefinition = "TEXT", nullable = false)
    private String certificatePdfUrl;

    @Column(name = "qr_verification_code", unique = true, nullable = false, length = 100)
    private String qrVerificationCode;

    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private boolean isRevoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
