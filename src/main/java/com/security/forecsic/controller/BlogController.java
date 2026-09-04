package com.security.forecsic.controller;

import com.security.forecsic.dto.*;
import com.security.forecsic.model.Blog;
import com.security.forecsic.service.BlogService;
import com.security.forecsic.service.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/blogs", "/blogpost"})
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    // 1. GET /api/blogs & /blogpost (Public Feed)
    @GetMapping
    public ResponseEntity<BlogListResponse> getPublishedBlogs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer limit
    ) {
        BlogListResponse response = blogService.getPublishedBlogs(category, search, page, limit);
        return ResponseEntity.ok(response);
    }

    // 2. GET /api/blogs/admin/all & /blogpost/admin/all (Admin Moderation)
    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLISHER', 'EDITOR')")
    public ResponseEntity<BlogAdminListResponse> getAdminBlogs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        BlogAdminListResponse response = blogService.getAdminBlogs(status, search);
        return ResponseEntity.ok(response);
    }

    // 3. GET /api/blogs/:id & /blogpost/:id (Public Blog Details + View Increment)
    @GetMapping("/{id}")
    public ResponseEntity<BlogResponse> getBlogById(@PathVariable String id) {
        Blog blog = blogService.getBlogByIdOrSlug(id);
        return ResponseEntity.ok(BlogResponse.builder()
                .success(true)
                .blog(blog)
                .build());
    }

    // 4. POST /api/blogs & /blogpost (Authenticated Creation)
    @PostMapping
    public ResponseEntity<BlogResponse> createBlog(
            @Valid @RequestBody BlogCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String email = userDetails != null ? userDetails.getUsername() : request.getAuthorEmail();
        String role = userDetails != null ? extractRole(userDetails) : request.getAuthorRole();
        Integer userId = userDetails != null ? userDetails.getId() : null;
        String name = request.getAuthor();

        BlogResponse response = blogService.createBlog(request, email, role, name, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 5. PUT /api/blogs/:id & /blogpost/:id (Full Edit)
    @PutMapping("/{id}")
    public ResponseEntity<BlogResponse> updateBlog(
            @PathVariable String id,
            @RequestBody BlogUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        String role = userDetails != null ? extractRole(userDetails) : "USER";

        BlogResponse response = blogService.updateBlog(id, request, email, role);
        return ResponseEntity.ok(response);
    }

    // 6. PATCH /api/blogs/:id/status & /blogpost/:id/status (Publish / Restrict / Reject)
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLISHER')")
    public ResponseEntity<BlogResponse> updateBlogStatus(
            @PathVariable String id,
            @Valid @RequestBody BlogStatusRequest request
    ) {
        BlogResponse response = blogService.updateBlogStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    // 7. DELETE /api/blogs/:id & /blogpost/:id (Delete & Cleanup Cloudinary Media)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLISHER', 'EDITOR')")
    public ResponseEntity<ApiResponse> deleteBlog(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        String role = userDetails != null ? extractRole(userDetails) : "USER";

        ApiResponse response = blogService.deleteBlog(id, email, role);
        return ResponseEntity.ok(response);
    }

    private String extractRole(CustomUserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .orElse("USER");
    }
}
