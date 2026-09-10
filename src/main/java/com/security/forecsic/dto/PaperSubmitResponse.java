package com.security.forecsic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperSubmitResponse {
    private boolean success;
    private String message;
    private String submissionId;
    private UUID paperId;
    private String title;
    private String status;
    private String emailSentTo;
}
