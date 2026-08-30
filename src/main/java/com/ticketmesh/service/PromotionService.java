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
        if (promotionRepository.findByTenantIdAndCode(tenantId, code.trim().toUpperCase())
                .isPresent()) {
            throw new ConflictException("Promotion code already exists: " + code);
        }
        return promotionRepository.save(new Promotion(
                tenantId, code.trim().toUpperCase(), name, type, value, minPurchase,
                startsAt, endsAt, maxUses, domains));
    }

    @Transactional(readOnly = true)
    public List<Promotion> listByTenant(Long tenantId) {
        return promotionRepository.findByTenantId(tenantId);
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
        Promotion p = promotionRepository
                .findByTenantIdAndCode(tenantId, code.trim().toUpperCase())
                .orElse(null);
        return isValid(p, subtotal) ? p : null;
    }

    /**
     * Validate + consume (increment usage). Returns the promotion or null.
     */
    @Transactional
    public Promotion redeem(Long tenantId, String code, BigDecimal subtotal) {
        Promotion p = promotionRepository
                .findByTenantIdAndCode(tenantId, code.trim().toUpperCase())
                .orElse(null);
        if (!isValid(p, subtotal)) {
            return null;
        }
        p.incrementUsed();
        return promotionRepository.save(p);
    }

    private boolean isValid(Promotion p, BigDecimal subtotal) {
        if (p == null || !p.isActiveNow()) {
            return false;
        }
        if (p.getMaxUses() != null && p.getUsedCount() >= p.getMaxUses()) {
            return false;
        }
        return p.getMinPurchase() == null || subtotal.compareTo(p.getMinPurchase()) >= 0;
    }
}
