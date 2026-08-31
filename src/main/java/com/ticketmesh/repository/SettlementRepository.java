package com.ticketmesh.repository;

import com.ticketmesh.model.SettlementEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SettlementRepository extends JpaRepository<SettlementEntry, Long> {
    List<SettlementEntry> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    List<SettlementEntry> findByOrderRef(String orderRef);
}
