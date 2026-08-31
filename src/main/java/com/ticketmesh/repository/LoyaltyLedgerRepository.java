package com.ticketmesh.repository;

import com.ticketmesh.model.LoyaltyLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoyaltyLedgerRepository extends JpaRepository<LoyaltyLedgerEntry, Long> {
    List<LoyaltyLedgerEntry> findByTenantIdAndUserIdOrderByCreatedAtDesc(Long tenantId, Long userId);
}
