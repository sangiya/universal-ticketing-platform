package com.ticketmesh.repository;

import com.ticketmesh.model.IdentityVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdentityVerificationRepository extends JpaRepository<IdentityVerification, Long> {

    Optional<IdentityVerification> findByUser_Id(Long userId);
}
