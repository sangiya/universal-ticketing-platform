package com.ticketmesh.service;

import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.Promotion;
import com.ticketmesh.model.Promotion.DiscountType;
import com.ticketmesh.repository.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Promotion engine. Admins create configurable coupon rules per tenant; the
 * engine validates and consumes them at checkout.
 */
@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Transactional
    public Promotion create(Long tenantId, String code, String name, DiscountType type,
                            BigDecimal value, BigDecimal minPurchase, Instant startsAt,
                            Instant endsAt, Integer maxUses, String domains) {
        return create(tenantId, code, name, type, value, minPurchase, startsAt, endsAt,
                maxUses, domains, Promotion.Kind.PROMO);
    }

    @Transactional
    public Promotion create(Long tenantId, String code, String name, DiscountType type,
                            BigDecimal value, BigDecimal minPurchase, Instant startsAt,
                            Instant endsAt, Integer maxUses, String domains, Promotion.Kind kind) {
        if (promotionRepository.findByTenantIdAndCode(tenantId, code.trim().toUpperCase())
                .isPresent()) {
            throw new ConflictException("Promotion code already exists: " + code);
        }
        return promotionRepository.save(new Promotion(
                tenantId, code.trim().toUpperCase(), name, type, value, minPurchase,
                startsAt, endsAt, maxUses, domains, kind));
    }

    @Transactional(readOnly = true)
    public List<Promotion> listByTenant(Long tenantId) {
        return promotionRepository.findByTenantId(tenantId);
    }

    /** Returns all currently-active discounts across all tenants (for the public home page). */
    @Transactional(readOnly = true)
    public List<Promotion> listPublicActive() {
        return promotionRepository.findActivePromotions(Instant.now());
    }

    @Transactional
    public Promotion setEnabled(Long promotionId, boolean enabled) {
        Promotion p = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new NotFoundException("Promotion not found: " + promotionId));
        p.setEnabled(enabled);
        return promotionRepository.save(p);
    }

    /**
     * Validate-only: returns the promotion if valid+active, else null. Does NOT
     * consume usage (used for price previews).
     */
    @Transactional(readOnly = true)
    public Promotion validate(Long tenantId, String code, BigDecimal subtotal) {
        return validate(tenantId, code, subtotal, null);
    }

    @Transactional(readOnly = true)
    public Promotion validate(Long tenantId, String code, BigDecimal subtotal, String productType) {
        Promotion p = promotionRepository
                .findByTenantIdAndCode(tenantId, code.trim().toUpperCase())
                .orElse(null);
        return isValid(p, subtotal, productType) ? p : null;
    }

    /**
     * Validate + consume (increment usage). Returns the promotion or null.
     */
    @Transactional
    public Promotion redeem(Long tenantId, String code, BigDecimal subtotal) {
        return redeem(tenantId, code, subtotal, null);
    }

    @Transactional
    public Promotion redeem(Long tenantId, String code, BigDecimal subtotal, String productType) {
        Promotion p = promotionRepository
                .findByTenantIdAndCode(tenantId, code.trim().toUpperCase())
                .orElse(null);
        if (!isValid(p, subtotal, productType)) {
            return null;
        }
        p.incrementUsed();
        return promotionRepository.save(p);
    }

    private boolean isValid(Promotion p, BigDecimal subtotal) {
        return isValid(p, subtotal, null);
    }

    private boolean isValid(Promotion p, BigDecimal subtotal, String productType) {
        if (p == null || !p.isActiveNow()) {
            return false;
        }
        if (p.getMaxUses() != null && p.getUsedCount() >= p.getMaxUses()) {
            return false;
        }
        if (p.getMinPurchase() != null && subtotal.compareTo(p.getMinPurchase()) < 0) {
            return false;
        }
        if (p.getDomains() != null && !p.getDomains().isBlank() && productType != null) {
            String domainsRaw = p.getDomains().trim();
            if (domainsRaw.equalsIgnoreCase("ALL") || domainsRaw.equals("*")) {
                // wildcard — matches any product type
            } else {
                String needed = productType.trim().toUpperCase();
                boolean match = java.util.Arrays.stream(domainsRaw.split(","))
                        .map(String::trim).map(String::toUpperCase)
                        .anyMatch(d -> d.equals(needed) || d.equals("*") || d.equals("ALL"));
                if (!match) return false;
            }
        }
        return true;
    }
}
