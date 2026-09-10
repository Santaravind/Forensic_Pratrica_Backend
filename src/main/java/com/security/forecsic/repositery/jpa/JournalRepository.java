package com.security.forecsic.repositery.jpa;

import com.security.forecsic.model.Journal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalRepository extends JpaRepository<Journal, UUID> {
    Optional<Journal> findByCode(String code);
    List<Journal> findByIsActiveTrueOrderByCreatedAtDesc();
    long countByIsActiveTrue();
}
