package com.security.forecsic.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaperAuthorDto {

    @JsonAlias({"name", "authorName", "fullName", "author_name"})
    private String name;

    @JsonAlias({"email", "authorEmail", "author_email"})
    private String email;

    @JsonAlias({"university", "organization", "affiliation", "college", "dept"})
    private String university;

    @JsonAlias({"isFirstAuthor", "firstAuthor", "is_first_author"})
    private boolean isFirstAuthor;

    @JsonAlias({"isCorrespondingAuthor", "correspondingAuthor", "is_corresponding_author"})
    private boolean isCorrespondingAuthor;

    @JsonAlias({"authorOrder", "order", "author_order"})
    private Integer authorOrder;
}
