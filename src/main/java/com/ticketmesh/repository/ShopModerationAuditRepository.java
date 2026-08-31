package com.ticketmesh.repository;

import com.ticketmesh.model.ShopModerationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShopModerationAuditRepository extends JpaRepository<ShopModerationAudit, Long> {
    List<ShopModerationAudit> findByShopIdOrderByCreatedAtDesc(Long shopId);
}
