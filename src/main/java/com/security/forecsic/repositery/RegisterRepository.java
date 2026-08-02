package com.security.forecsic.repositery;


import com.security.forecsic.model.Register;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegisterRepository extends JpaRepository<Register, Integer> {
    Optional<Register> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByMobileNo(Long mobileNo);
}