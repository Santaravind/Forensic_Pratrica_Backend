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
public class PublishedPaperResponse {
    private UUID id;
    private UUID manuscriptId;
    private String submissionId;
    private String title;
    private String journal;
    private String author;
    private String date; // formatted e.g. "15 May 2026"
    private String issue;
    private String doi;
    private String pdfUrl;
    private Integer downloadsCount;
    private Integer viewsCount;
}
