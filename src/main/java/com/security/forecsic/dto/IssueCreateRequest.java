package com.security.forecsic.dto;

import jakarta.validation.constraints.NotBlank;
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
public class IssueCreateRequest {

    @NotNull(message = "journalId is required")
    private UUID journalId;

    @NotNull(message = "volumeNo is required")
    private Integer volumeNo;

    @NotNull(message = "issueNo is required")
    private Integer issueNo;

    private String issueTitle;

    @NotNull(message = "year is required")
    private Integer year;

    @NotBlank(message = "month is required")
    private String month;

    private String coverImageUrl;
}
