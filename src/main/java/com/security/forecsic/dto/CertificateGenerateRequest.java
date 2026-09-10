package com.security.forecsic.dto;

import com.security.forecsic.model.CertificateType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateGenerateRequest {

    @NotNull(message = "manuscriptId is required")
    private UUID manuscriptId;

    private Integer recipientUserId;
    private String recipientName;
    private String recipientEmail;
    private CertificateType certificateType;
}
