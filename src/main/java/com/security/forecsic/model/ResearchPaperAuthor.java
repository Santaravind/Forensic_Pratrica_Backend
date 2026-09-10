package com.security.forecsic.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "researchpaper_authors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResearchPaperAuthor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "research_paper_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @JsonIgnore
    private ResearchPaper researchPaper;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(length = 255)
    private String university;

    @Column(name = "is_first_author", nullable = false)
    @Builder.Default
    private boolean isFirstAuthor = false;

    @Column(name = "is_corresponding_author", nullable = false)
    @Builder.Default
    private boolean isCorrespondingAuthor = false;

    @Column(name = "author_order", nullable = false)
    @Builder.Default
    private Integer authorOrder = 1;
}
