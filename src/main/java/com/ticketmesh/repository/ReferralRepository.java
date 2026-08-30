package com.ticketmesh.repository;

import com.ticketmesh.model.Referral;
import com.ticketmesh.model.Referral.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    List<Referral> findByReferrerUserId(Long referrerUserId);

    boolean existsByCode(String code);

    Optional<Referral> findByCodeAndStatus(String code, Status status);
}
