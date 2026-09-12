package com.security.forecsic.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.security.forecsic.dto.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final Cloudinary cloudinary;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "png", "jpg", "jpeg"
    );

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "application/octet-stream" // for raw document uploads from some browsers
    );

    private static final Set<String> ALLOWED_FOLDERS = Set.of(
            "research_papers",
            "published_papers",
            "certificates",
            "blogs",
            "journal_covers"
    );

    /**
     * Upload DOC, DOCX, PDF, or Image file to Cloudinary
     */
    public FileUploadResponse uploadFile(MultipartFile file, String folderName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "document_" + UUID.randomUUID();
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file type ." + extension + ". Supported formats are DOC, DOCX, PDF, PNG, JPG.");
        }

        // Validate MIME type
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file content type: " + contentType);
        }

        // Whitelist destination folder to prevent path traversal
        String targetFolder = (folderName != null && ALLOWED_FOLDERS.contains(folderName.trim().toLowerCase()))
                ? folderName.trim().toLowerCase()
                : "research_papers";

        String cleanBaseName = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        String publicId = targetFolder + "/" + UUID.randomUUID() + "_" + cleanBaseName;

        try {
            // Use 'raw' resource_type for doc, docx, pdf; 'auto' for others
            String resourceType = (extension.equals("doc") || extension.equals("docx") || extension.equals("pdf")) ? "raw" : "auto";

            Map<String, Object> params = ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", resourceType,
                    "use_filename", true,
                    "unique_filename", true
            );

            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            String secureUrl = (String) uploadResult.get("secure_url");
            if (secureUrl == null) {
                secureUrl = (String) uploadResult.get("url");
            }

            log.info("Successfully uploaded file {} as {} to Cloudinary. URL: {}", originalFilename, resourceType, secureUrl);

            return FileUploadResponse.builder()
                    .success(true)
                    .fileUrl(secureUrl)
                    .fileName(originalFilename)
                    .fileType(extension)
                    .fileSize(file.getSize())
                    .message("File uploaded successfully")
                    .build();
        } catch (IOException e) {
            log.error("Cloudinary upload failed for file {}", originalFilename, e);
            throw new RuntimeException("Failed to upload file to Cloudinary: " + e.getMessage(), e);
        }
    }

    public static String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDotIndex = filename.lastIndexOf('.');
        return (lastDotIndex != -1 && lastDotIndex < filename.length() - 1) ? filename.substring(lastDotIndex + 1) : "";
    }
}
