package com.security.forecsic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalResponse {
    private UUID id;
    private String title;
    private String code;
    private String issnPrint;
    private String issnOnline;
    private String description;
    private String aimsScope;
    private String coverImageUrl;
    private boolean isActive;
    private long totalIssues;
    private long totalPapers;
    private Instant createdAt;
    private Instant updatedAt;
}
