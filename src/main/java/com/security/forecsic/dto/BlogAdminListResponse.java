package com.security.forecsic.dto;

import com.security.forecsic.model.Blog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogAdminListResponse {
    private boolean success;
    private int count;
    private List<Blog> blogs;
}
