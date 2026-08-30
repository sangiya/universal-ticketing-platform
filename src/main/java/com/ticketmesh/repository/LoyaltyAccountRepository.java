package com.ticketmesh.repository;

import com.ticketmesh.model.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {

    Optional<LoyaltyAccount> findByTenantIdAndUserId(Long tenantId, Long userId);
}
