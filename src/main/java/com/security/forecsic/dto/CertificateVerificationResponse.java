package com.security.forecsic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateVerificationResponse {
    private boolean valid;
    private String message;
    private String certificateNo;
    private String recipientName;
    private String paperTitle;
    private String journalTitle;
    private String issueTitle;
    private String doi;
    private LocalDate issueDate;
    private String certificateType;
    private String certificatePdfUrl;
    private boolean isRevoked;
}
