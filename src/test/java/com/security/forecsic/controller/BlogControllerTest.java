package com.security.forecsic.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.security.forecsic.dto.*;
import com.security.forecsic.model.Blog;
import com.security.forecsic.service.BlogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BlogControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BlogService blogService;

    @InjectMocks
    private BlogController blogController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(blogController).build();
    }

    @Test
    void testGetPublishedBlogs() throws Exception {
        Blog blog = Blog.builder()
                .id("664f3c8a9e1a2b3c4d5e6f7a")
                .title("Forensic Analysis")
                .status("published")
                .build();

        BlogListResponse response = BlogListResponse.builder()
                .success(true)
                .count(1)
                .total(1)
                .page(1)
                .totalPages(1)
                .blogs(List.of(blog))
                .build();

        when(blogService.getPublishedBlogs(any(), any(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/blogs")
                        .param("page", "1")
                        .param("limit", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.blogs[0].title").value("Forensic Analysis"));
    }

    @Test
    void testGetBlogById() throws Exception {
        Blog blog = Blog.builder()
                .id("664f3c8a9e1a2b3c4d5e6f7a")
                .title("Forensic DNA Analysis")
                .views(10L)
                .build();

        when(blogService.getBlogByIdOrSlug("664f3c8a9e1a2b3c4d5e6f7a")).thenReturn(blog);

        mockMvc.perform(get("/api/blogs/664f3c8a9e1a2b3c4d5e6f7a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.blog.title").value("Forensic DNA Analysis"))
                .andExpect(jsonPath("$.blog.views").value(10));
    }

    @Test
    void testPatchBlogStatus() throws Exception {
        Blog blog = Blog.builder()
                .id("664f3c8a9e1a2b3c4d5e6f7a")
                .status("restricted")
                .build();

        BlogResponse response = BlogResponse.builder()
                .success(true)
                .message("Article status changed to 'restricted'.")
                .blog(blog)
                .build();

        when(blogService.updateBlogStatus(eq("664f3c8a9e1a2b3c4d5e6f7a"), eq("restricted"))).thenReturn(response);

        BlogStatusRequest request = new BlogStatusRequest("restricted");

        mockMvc.perform(patch("/api/blogs/664f3c8a9e1a2b3c4d5e6f7a/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Article status changed to 'restricted'."))
                .andExpect(jsonPath("$.blog.status").value("restricted"));
    }

    @Test
    void testDeleteBlog() throws Exception {
        ApiResponse response = new ApiResponse(true, "Article and associated assets deleted successfully.");

        when(blogService.deleteBlog(eq("664f3c8a9e1a2b3c4d5e6f7a"), any(), any())).thenReturn(response);

        mockMvc.perform(delete("/api/blogs/664f3c8a9e1a2b3c4d5e6f7a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Article and associated assets deleted successfully."));
    }
}
