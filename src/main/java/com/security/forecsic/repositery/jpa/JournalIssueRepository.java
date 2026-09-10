package com.security.forecsic.repositery.jpa;

import com.security.forecsic.model.JournalIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalIssueRepository extends JpaRepository<JournalIssue, UUID> {
    List<JournalIssue> findByJournalIdOrderByYearDescVolumeNoDescIssueNoDesc(UUID journalId);
    List<JournalIssue> findAllByOrderByYearDescVolumeNoDescIssueNoDesc();
    long countByIsPublishedTrue();
    Optional<JournalIssue> findByJournalIdAndVolumeNoAndIssueNo(UUID journalId, Integer volumeNo, Integer issueNo);
}
