package com.security.forecsic.repositery.jpa;

import com.security.forecsic.model.ResearchPaper;
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
public interface ResearchPaperRepository extends JpaRepository<ResearchPaper, UUID>, JpaSpecificationExecutor<ResearchPaper> {

    Optional<ResearchPaper> findBySubmissionId(String submissionId);

    Optional<ResearchPaper> findByDoi(String doi);

    List<ResearchPaper> findByStatusOrderByUpdatedAtAsc(String status);

    Page<ResearchPaper> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);

    @Query("SELECT COUNT(r) FROM ResearchPaper r")
    long countTotal();
}
