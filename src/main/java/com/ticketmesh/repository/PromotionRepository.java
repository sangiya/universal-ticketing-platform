package com.ticketmesh.repository;

import com.ticketmesh.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findByTenantId(Long tenantId);

    Optional<Promotion> findByTenantIdAndCode(Long tenantId, String code);
}
