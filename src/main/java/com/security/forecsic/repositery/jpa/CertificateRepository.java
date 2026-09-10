package com.security.forecsic.repositery.jpa;

import com.security.forecsic.model.Certificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID>, JpaSpecificationExecutor<Certificate> {

    Optional<Certificate> findByQrVerificationCode(String qrVerificationCode);

    Optional<Certificate> findByCertificateNo(String certificateNo);

    List<Certificate> findByResearchPaperId(UUID researchPaperId);

    List<Certificate> findByRecipientEmailOrderByIssueDateDesc(String recipientEmail);
}
