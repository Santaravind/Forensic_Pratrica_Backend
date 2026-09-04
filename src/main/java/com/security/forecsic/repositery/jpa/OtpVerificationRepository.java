package com.security.forecsic.repositery.jpa;

import com.security.forecsic.model.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findTopByEmailAndTypeOrderByCreatedAtDesc(String email, String type);
    void deleteByEmailAndType(String email, String type);
}
