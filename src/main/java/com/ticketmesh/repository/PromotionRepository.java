package com.ticketmesh.repository;

import com.ticketmesh.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findByTenantId(Long tenantId);

    Optional<Promotion> findByTenantIdAndCode(Long tenantId, String code);

    /**
     * Returns active, currently-valid discount codes across all tenants.
     * Used by the public "discounts" section of the home page so visitors
     * can see what coupon codes are available before signing in.
     */
    @Query("SELECT p FROM Promotion p WHERE p.enabled = true " +
           "AND (p.startsAt IS NULL OR p.startsAt <= :now) " +
           "AND (p.endsAt IS NULL OR p.endsAt >= :now) " +
           "ORDER BY p.kind ASC, p.createdAt DESC")
    List<Promotion> findActivePromotions(@Param("now") Instant now);
}
