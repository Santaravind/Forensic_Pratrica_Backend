package com.security.forecsic.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.security.forecsic.dto.*;
import com.security.forecsic.exception.ResourceNotFoundException;
import com.security.forecsic.model.Blog;
import com.security.forecsic.model.BlogImage;
import com.security.forecsic.repositery.jpa.BlogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogService {

    private final BlogRepository blogRepository;
    private final Cloudinary cloudinary;

    private static final List<String> ALLOWED_STATUSES = Arrays.asList(
            "published", "restricted", "pending", "rejected", "draft"
    );

    private static final List<String> PRIVILEGED_ROLES = Arrays.asList(
            "ADMIN", "PUBLISHER", "EDITOR"
    );

    @Transactional(readOnly = true)
    public BlogListResponse getPublishedBlogs(String category, String search, Integer page, Integer limit) {
        int pageNum = (page == null || page < 1) ? 1 : page;
        int limitNum = (limit == null || limit < 1) ? 12 : limit;

        Pageable pageable = PageRequest.of(pageNum - 1, limitNum, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Blog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(cb.lower(root.get("status")), "published"));

            if (category != null && !category.isBlank() && !category.equalsIgnoreCase("All")) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }

            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), term);
                Predicate summaryLike = cb.like(cb.lower(root.get("summary")), term);
                Predicate contentLike = cb.like(cb.lower(root.get("content")), term);
                Predicate authorLike = cb.like(cb.lower(root.get("author")), term);

                predicates.add(cb.or(titleLike, summaryLike, contentLike, authorLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Blog> blogPage = blogRepository.findAll(spec, pageable);

        return BlogListResponse.builder()
                .success(true)
                .count(blogPage.getNumberOfElements())
                .total(blogPage.getTotalElements())
                .page(pageNum)
                .totalPages(blogPage.getTotalPages())
                .blogs(blogPage.getContent())
                .build();
    }

    @Transactional
    public Blog getBlogByIdOrSlug(String idOrSlug) {
        if (idOrSlug == null || idOrSlug.isBlank()) {
            throw new ResourceNotFoundException("Article identifier is required.");
        }

        Blog blog = blogRepository.findById(idOrSlug)
                .or(() -> blogRepository.findBySlug(idOrSlug))
                .orElseThrow(() -> new ResourceNotFoundException("Article not found."));

        blog.setViews(blog.getViews() != null ? blog.getViews() + 1 : 1L);
        return blogRepository.save(blog);
    }

    @Transactional
    public BlogResponse createBlog(BlogCreateRequest req, String userEmail, String userRole, String userName, Integer userId) {
        String role = (userRole != null && !userRole.isBlank()) ? userRole.toUpperCase() : "USER";
        boolean isPrivileged = PRIVILEGED_ROLES.contains(role);

        String status = isPrivileged
                ? (req.getStatus() != null && !req.getStatus().isBlank() ? req.getStatus().toLowerCase() : "published")
                : "pending";

        String slug = (req.getSlug() != null && !req.getSlug().isBlank())
                ? req.getSlug().toLowerCase().trim()
                : generateSlug(req.getTitle());

        String readTime = (req.getReadTime() != null && !req.getReadTime().isBlank())
                ? req.getReadTime()
                : calculateReadTime(req.getContent());

        String publishDate = (req.getPublishDate() != null && !req.getPublishDate().isBlank())
                ? req.getPublishDate()
                : LocalDate.now().toString();

        String author = (req.getAuthor() != null && !req.getAuthor().isBlank())
                ? req.getAuthor()
                : (userName != null ? userName : "Author");

        String authorEmail = (userEmail != null && !userEmail.isBlank())
                ? userEmail
                : req.getAuthorEmail();

        String authorId = (userId != null)
                ? String.valueOf(userId)
                : req.getAuthorId();

        Instant now = Instant.now();

        Blog blog = Blog.builder()
                .title(req.getTitle())
                .slug(slug)
                .category(req.getCategory() != null && !req.getCategory().isBlank() ? req.getCategory() : "Forensic Science")
                .author(author)
                .authorEmail(authorEmail)
                .authorId(authorId)
                .authorRole(role)
                .publishDate(publishDate)
                .summary(req.getSummary())
                .content(req.getContent())
                .readTime(readTime)
                .images(req.getImages() != null ? req.getImages() : new ArrayList<>())
                .tags(req.getTags() != null ? req.getTags() : new ArrayList<>())
                .status(status)
                .views(0L)
                .likes(0L)
                .isFeatured(req.getIsFeatured() != null ? req.getIsFeatured() : false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Blog saved = blogRepository.save(blog);

        String message = isPrivileged
                ? "Article published successfully!"
                : "Article submitted for editorial review.";

        return BlogResponse.builder()
                .success(true)
                .message(message)
                .blog(saved)
                .build();
    }

    @Transactional(readOnly = true)
    public BlogAdminListResponse getAdminBlogs(String status, String search) {
        Specification<Blog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
                predicates.add(cb.equal(cb.lower(root.get("status")), status.toLowerCase().trim()));
            }

            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), term);
                Predicate authorLike = cb.like(cb.lower(root.get("author")), term);
                Predicate categoryLike = cb.like(cb.lower(root.get("category")), term);

                predicates.add(cb.or(titleLike, authorLike, categoryLike));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Blog> blogs = blogRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));

        return BlogAdminListResponse.builder()
                .success(true)
                .count(blogs.size())
                .blogs(blogs)
                .build();
    }

    @Transactional
    public BlogResponse updateBlog(String id, BlogUpdateRequest req, String userEmail, String userRole) {
        Blog blog = findBlogByIdOrSlug(id);

        String role = (userRole != null) ? userRole.toUpperCase() : "USER";
        boolean isPrivileged = PRIVILEGED_ROLES.contains(role);
        boolean isAuthor = userEmail != null && userEmail.equalsIgnoreCase(blog.getAuthorEmail());

        if (!isPrivileged && !isAuthor) {
            throw new AccessDeniedException("Forbidden: You are not authorized to update this article.");
        }

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            blog.setTitle(req.getTitle());
            if (blog.getSlug() == null || blog.getSlug().isBlank()) {
                blog.setSlug(generateSlug(req.getTitle()));
            }
        }
        if (req.getCategory() != null && !req.getCategory().isBlank()) {
            blog.setCategory(req.getCategory());
        }
        if (req.getAuthor() != null && !req.getAuthor().isBlank()) {
            blog.setAuthor(req.getAuthor());
        }
        if (req.getSummary() != null) {
            blog.setSummary(req.getSummary());
        }
        if (req.getContent() != null && !req.getContent().isBlank()) {
            blog.setContent(req.getContent());
            blog.setReadTime(calculateReadTime(req.getContent()));
        }
        if (req.getReadTime() != null && !req.getReadTime().isBlank()) {
            blog.setReadTime(req.getReadTime());
        }
        if (req.getImages() != null) {
            blog.setImages(req.getImages());
        }
        if (req.getTags() != null) {
            blog.setTags(req.getTags());
        }
        if (req.getIsFeatured() != null) {
            blog.setIsFeatured(req.getIsFeatured());
        }
        if (req.getPublishDate() != null && !req.getPublishDate().isBlank()) {
            blog.setPublishDate(req.getPublishDate());
        }
        if (isPrivileged && req.getStatus() != null && !req.getStatus().isBlank()) {
            if (ALLOWED_STATUSES.contains(req.getStatus().toLowerCase())) {
                blog.setStatus(req.getStatus().toLowerCase());
            }
        }

        blog.setUpdatedAt(Instant.now());
        Blog updated = blogRepository.save(blog);

        return BlogResponse.builder()
                .success(true)
                .message("Article updated successfully.")
                .blog(updated)
                .build();
    }

    @Transactional
    public BlogResponse updateBlogStatus(String id, String status) {
        if (status == null || !ALLOWED_STATUSES.contains(status.toLowerCase().trim())) {
            throw new IllegalArgumentException("Invalid status. Must be one of: " + String.join(", ", ALLOWED_STATUSES));
        }

        Blog blog = findBlogByIdOrSlug(id);
        blog.setStatus(status.toLowerCase().trim());
        blog.setUpdatedAt(Instant.now());

        Blog saved = blogRepository.save(blog);

        return BlogResponse.builder()
                .success(true)
                .message("Article status changed to '" + status.toLowerCase().trim() + "'.")
                .blog(saved)
                .build();
    }

    @Transactional
    public ApiResponse deleteBlog(String id, String userEmail, String userRole) {
        Blog blog = findBlogByIdOrSlug(id);

        String role = (userRole != null) ? userRole.toUpperCase() : "USER";
        boolean isPrivileged = PRIVILEGED_ROLES.contains(role);
        boolean isAuthor = userEmail != null && userEmail.equalsIgnoreCase(blog.getAuthorEmail());

        if (!isPrivileged && !isAuthor) {
            throw new AccessDeniedException("Forbidden: You are not authorized to delete this article.");
        }

        // Cleanup images from Cloudinary
        if (blog.getImages() != null && !blog.getImages().isEmpty()) {
            for (BlogImage img : blog.getImages()) {
                if (img.getPublicId() != null && !img.getPublicId().isBlank()) {
                    try {
                        cloudinary.uploader().destroy(img.getPublicId(), ObjectUtils.emptyMap());
                        log.info("Successfully deleted Cloudinary asset: {}", img.getPublicId());
                    } catch (Exception cloudErr) {
                        log.warn("Could not delete Cloudinary asset {}: {}", img.getPublicId(), cloudErr.getMessage());
                    }
                }
            }
        }

        blogRepository.delete(blog);

        return new ApiResponse(true, "Article and associated assets deleted successfully.");
    }

    private Blog findBlogByIdOrSlug(String idOrSlug) {
        if (idOrSlug == null || idOrSlug.isBlank()) {
            throw new ResourceNotFoundException("Article identifier is required.");
        }

        return blogRepository.findById(idOrSlug)
                .or(() -> blogRepository.findBySlug(idOrSlug))
                .orElseThrow(() -> new ResourceNotFoundException("Article not found."));
    }

    private String generateSlug(String title) {
        if (title == null || title.isBlank()) {
            return "article-" + Long.toString(System.currentTimeMillis(), 36);
        }
        String base = title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)+", "");
        return base + "-" + Long.toString(System.currentTimeMillis(), 36);
    }

    private String calculateReadTime(String content) {
        if (content == null || content.isBlank()) {
            return "1 min read";
        }
        int wordCount = content.trim().split("\\s+").length;
        int minutes = Math.max(1, (int) Math.ceil((double) wordCount / 180.0));
        return minutes + " min read";
    }
}
