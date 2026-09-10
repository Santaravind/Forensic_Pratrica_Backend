package com.security.forecsic.repositery.jpa;

import com.security.forecsic.model.Publication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PublicationRepository extends JpaRepository<Publication, UUID>, JpaSpecificationExecutor<Publication> {

    Optional<Publication> findByDoi(String doi);

    Optional<Publication> findByResearchPaperId(UUID researchPaperId);

    List<Publication> findByJournalId(UUID journalId);

    List<Publication> findByIssueId(UUID issueId);

    @Query("SELECT COALESCE(SUM(p.downloadsCount), 0) FROM Publication p")
    long sumDownloads();

    @Query("SELECT COALESCE(SUM(p.viewsCount), 0) FROM Publication p")
    long sumViews();

    long countByJournalId(UUID journalId);
    long countByIssueId(UUID issueId);
}
