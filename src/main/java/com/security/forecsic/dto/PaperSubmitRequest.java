package com.security.forecsic.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaperSubmitRequest {

    @NotBlank(message = "Title is required")
    @JsonAlias({"title", "paperTitle", "name"})
    private String title;

    @JsonAlias({"researchArea", "research_area", "domain", "category", "subject"})
    private String researchArea;

    @JsonAlias({"abstractText", "abstract", "abstract_text", "description", "summary"})
    private String abstractText;

    @JsonAlias({"keywords", "tags"})
    private String keywords;

    @JsonAlias({"manuscriptFileUrl", "manuscript_file_url", "menuScript", "manuscript", "fileUrl", "file"})
    private String manuscriptFileUrl;

    @JsonAlias({"manuscriptFileType", "manuscript_file_type", "fileType", "type"})
    private String manuscriptFileType; // 'pdf', 'docx', 'doc'

    @JsonAlias({"authors", "author", "authorList", "authorDetails"})
    private List<PaperAuthorDto> authors;
}
