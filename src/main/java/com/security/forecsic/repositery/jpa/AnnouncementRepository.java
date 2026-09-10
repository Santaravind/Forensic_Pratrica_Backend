package com.security.forecsic.repositery.jpa;

import com.security.forecsic.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {
    List<Announcement> findByIsActiveTrueOrderByCreatedAtDesc();
    List<Announcement> findByTargetRoleInAndIsActiveTrueOrderByCreatedAtDesc(List<String> targetRoles);
    List<Announcement> findByIsActiveTrueAndExpiresAtAfterOrExpiresAtIsNullOrderByCreatedAtDesc(Instant now);
}
