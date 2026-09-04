package com.security.forecsic.dto;

import com.security.forecsic.model.BlogImage;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogUpdateRequest {

    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    private String category;

    private String author;

    private String authorEmail;

    private String authorRole;

    private String publishDate;

    @Size(max = 500, message = "Summary cannot exceed 500 characters")
    private String summary;

    private String content;

    private String readTime;

    private List<BlogImage> images;

    private List<String> tags;

    private String status;

    private Boolean isFeatured;
}
