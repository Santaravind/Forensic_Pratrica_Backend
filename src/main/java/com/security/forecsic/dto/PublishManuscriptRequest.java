package com.security.forecsic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishManuscriptRequest {

    @NotNull(message = "manuscriptId is required")
    private UUID manuscriptId;

    @NotNull(message = "journalId is required")
    private UUID journalId;

    @NotNull(message = "issueId is required")
    private UUID issueId;

    @NotBlank(message = "doi is required")
    private String doi;

    private LocalDate publishedDate;

    private Integer startPage;

    private Integer endPage;

    @NotBlank(message = "finalPdfUrl is required")
    private String finalPdfUrl;

    @Builder.Default
    private Boolean generateCertificates = true;

    @Builder.Default
    private Boolean sendNotificationEmail = true;

    public boolean isGenerateCertificates() {
        return !Boolean.FALSE.equals(this.generateCertificates);
    }

    public boolean isSendNotificationEmail() {
        return !Boolean.FALSE.equals(this.sendNotificationEmail);
    }
}
