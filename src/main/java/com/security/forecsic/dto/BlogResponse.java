package com.security.forecsic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.security.forecsic.model.Blog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlogResponse {
    private boolean success;
    private String message;
    private Blog blog;
}
