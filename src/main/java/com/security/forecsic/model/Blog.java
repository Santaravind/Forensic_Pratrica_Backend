package com.security.forecsic.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.security.forecsic.model.converter.BlogImageListConverter;
import com.security.forecsic.model.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "blogs", indexes = {
        @Index(name = "idx_blog_slug", columnList = "slug", unique = true),
        @Index(name = "idx_blog_status", columnList = "status"),
        @Index(name = "idx_blog_category", columnList = "category"),
        @Index(name = "idx_blog_author_email", columnList = "authorEmail"),
        @Index(name = "idx_blog_created_at", columnList = "created_at")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(unique = true, nullable = false, length = 350)
    private String slug;

    @Column(length = 100)
    @Builder.Default
    private String category = "Forensic Science";

    @Column(length = 150)
    private String author;

    @Column(name = "author_email", length = 150)
    private String authorEmail;

    @Column(name = "author_id", length = 100)
    private String authorId;

    @Column(name = "author_role", length = 50)
    @Builder.Default
    private String authorRole = "USER";

    @Column(name = "publish_date", length = 50)
    private String publishDate;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "read_time", length = 50)
    @Builder.Default
    private String readTime = "5 min read";

    @Convert(converter = BlogImageListConverter.class)
    @Column(name = "images", columnDefinition = "TEXT")
    @Builder.Default
    private List<BlogImage> images = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "tags", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(length = 50)
    @Builder.Default
    private String status = "published";

    @Column(nullable = false)
    @Builder.Default
    private Long views = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long likes = 0L;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (views == null) {
            views = 0L;
        }
        if (likes == null) {
            likes = 0L;
        }
        if (isFeatured == null) {
            isFeatured = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Helper for frontend compatibility expecting _id
    @JsonGetter("_id")
    public String getUnderscoreId() {
        return id;
    }
}
