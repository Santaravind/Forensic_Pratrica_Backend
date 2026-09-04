package com.security.forecsic.repositery.jpa;

import com.security.forecsic.model.Register;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegisterRepository extends JpaRepository<Register, Integer> {
    Optional<Register> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByMobileNo(Long mobileNo);
}
