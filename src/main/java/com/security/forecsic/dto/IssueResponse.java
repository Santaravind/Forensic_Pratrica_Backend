package com.security.forecsic.dto;

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
public class IssueResponse {
    private UUID id;
    private UUID journalId;
    private String journalTitle;
    private Integer volumeNo;
    private Integer issueNo;
    private String issueTitle;
    private Integer year;
    private String month;
    private String coverImageUrl;
    private boolean isPublished;
    private LocalDate publishedDate;
    private long papersCount;
}
