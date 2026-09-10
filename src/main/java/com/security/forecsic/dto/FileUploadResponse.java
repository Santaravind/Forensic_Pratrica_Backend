package com.security.forecsic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private boolean success;
    private String fileUrl;
    private String fileName;
    private String fileType; // 'pdf', 'docx', 'doc', etc.
    private Long fileSize;
    private String message;
}
