package com.security.forecsic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublisherQueueItemResponse {
    private UUID id;
    private String submissionId;
    private String title;
    private String researchArea;
    private String abstractText;
    private String status;
    private Instant acceptedAt;
    private Instant submittedAt;
    private PaperAuthorDto firstAuthor;
    private List<PaperAuthorDto> authors;
    private String manuscriptFileUrl;
    private String manuscriptFileType; // 'pdf', 'docx', 'doc'
}
