package com.security.forecsic.repositery.jpa;

import com.security.forecsic.model.ResearchPaperAuthor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResearchPaperAuthorRepository extends JpaRepository<ResearchPaperAuthor, UUID> {
    List<ResearchPaperAuthor> findByResearchPaperIdOrderByAuthorOrderAsc(UUID researchPaperId);
    Optional<ResearchPaperAuthor> findFirstByResearchPaperIdAndIsFirstAuthorTrue(UUID researchPaperId);
}
