package com.security.forecsic.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.security.forecsic.dto.*;
import com.security.forecsic.exception.ResourceNotFoundException;
import com.security.forecsic.model.Blog;
import com.security.forecsic.model.BlogImage;
import com.security.forecsic.repositery.jpa.BlogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock
    private BlogRepository blogRepository;

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private BlogService blogService;

    @BeforeEach
    void setUp() {
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
    }

    @Test
    void testCreateBlog_ByUser_SetsPendingStatus() {
        BlogCreateRequest request = BlogCreateRequest.builder()
                .title("Investigation into Digital Evidence")
                .content("Detailed content about forensics analysis in laboratory.")
                .category("Forensic Science")
                .build();

        when(blogRepository.save(any(Blog.class))).thenAnswer(invocation -> {
            Blog b = invocation.getArgument(0);
            b.setId("664f3c8a9e1a2b3c4d5e6f7a");
            return b;
        });

        BlogResponse response = blogService.createBlog(request, "user@example.com", "USER", "John Doe", 101);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Article submitted for editorial review.", response.getMessage());
        assertEquals("pending", response.getBlog().getStatus());
        assertEquals("John Doe", response.getBlog().getAuthor());
        assertEquals("user@example.com", response.getBlog().getAuthorEmail());
        assertNotNull(response.getBlog().getSlug());
        assertTrue(response.getBlog().getSlug().startsWith("investigation-into-digital-evidence-"));
        assertEquals("1 min read", response.getBlog().getReadTime());
    }

    @Test
    void testCreateBlog_ByAdmin_SetsPublishedStatus() {
        BlogCreateRequest request = BlogCreateRequest.builder()
                .title("Advanced Ballistics Examination")
                .content("In-depth ballistic trajectory and residue study...")
                .category("Ballistics")
                .status("published")
                .build();

        when(blogRepository.save(any(Blog.class))).thenAnswer(invocation -> {
            Blog b = invocation.getArgument(0);
            b.setId("664f3c8a9e1a2b3c4d5e6f7b");
            return b;
        });

        BlogResponse response = blogService.createBlog(request, "admin@example.com", "ADMIN", "Dr. Indresh", 1);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Article published successfully!", response.getMessage());
        assertEquals("published", response.getBlog().getStatus());
    }

    @Test
    void testGetBlogByIdOrSlug_IncrementsViews() {
        Blog mockBlog = Blog.builder()
                .id("664f3c8a9e1a2b3c4d5e6f7a")
                .title("Test Blog")
                .slug("test-blog-123")
                .views(5L)
                .build();

        when(blogRepository.findById("664f3c8a9e1a2b3c4d5e6f7a")).thenReturn(Optional.of(mockBlog));
        when(blogRepository.save(any(Blog.class))).thenAnswer(i -> i.getArgument(0));

        Blog result = blogService.getBlogByIdOrSlug("664f3c8a9e1a2b3c4d5e6f7a");

        assertNotNull(result);
        assertEquals("Test Blog", result.getTitle());
        assertEquals(6L, result.getViews());
    }

    @Test
    void testUpdateBlogStatus_ValidStatus() {
        Blog blog = Blog.builder()
                .id("664f3c8a9e1a2b3c4d5e6f7a")
                .status("published")
                .build();

        when(blogRepository.findById("664f3c8a9e1a2b3c4d5e6f7a")).thenReturn(Optional.of(blog));
        when(blogRepository.save(any(Blog.class))).thenAnswer(i -> i.getArgument(0));

        BlogResponse response = blogService.updateBlogStatus("664f3c8a9e1a2b3c4d5e6f7a", "restricted");

        assertTrue(response.isSuccess());
        assertEquals("Article status changed to 'restricted'.", response.getMessage());
        assertEquals("restricted", response.getBlog().getStatus());
    }

    @Test
    void testUpdateBlogStatus_InvalidStatus_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            blogService.updateBlogStatus("664f3c8a9e1a2b3c4d5e6f7a", "invalid_status");
        });
    }

    @Test
    void testDeleteBlog_DestroysCloudinaryImages() throws Exception {
        BlogImage img1 = BlogImage.builder().url("https://res.cloudinary.com/demo/image1.jpg").publicId("forensic/img1").build();
        BlogImage img2 = BlogImage.builder().url("https://res.cloudinary.com/demo/image2.jpg").publicId("forensic/img2").build();

        Blog blog = Blog.builder()
                .id("664f3c8a9e1a2b3c4d5e6f7a")
                .authorEmail("admin@example.com")
                .images(List.of(img1, img2))
                .build();

        when(blogRepository.findById("664f3c8a9e1a2b3c4d5e6f7a")).thenReturn(Optional.of(blog));

        ApiResponse response = blogService.deleteBlog("664f3c8a9e1a2b3c4d5e6f7a", "admin@example.com", "ADMIN");

        assertTrue(response.isSuccess());
        assertEquals("Article and associated assets deleted successfully.", response.getMessage());

        verify(uploader).destroy(eq("forensic/img1"), any(Map.class));
        verify(uploader).destroy(eq("forensic/img2"), any(Map.class));
        verify(blogRepository).delete(blog);
    }
}
