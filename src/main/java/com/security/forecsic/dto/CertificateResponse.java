package com.security.forecsic.dto;

import com.security.forecsic.model.CertificateType;
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
public class CertificateResponse {
    private UUID id;
    private String certificateNo;
    private String recipientName;
    private String recipientEmail;
    private UUID manuscriptId;
    private String paperTitle;
    private CertificateType certificateType;
    private LocalDate issueDate;
    private String certificatePdfUrl;
    private String qrVerificationCode;
    private boolean isRevoked;
}
