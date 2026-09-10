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
public class AnnouncementResponse {
    private UUID id;
    private String title;
    private String content;
    private String targetRole;
    private boolean isActive;
    private String createdByName;
    private Instant expiresAt;
    private Instant createdAt;
}
