package com.ticketmesh.service;

import com.ticketmesh.model.LoyaltyAccount;
import com.ticketmesh.repository.LoyaltyAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer loyalty: earn points on bookings, progress through tiers.
 */
@Service
public class LoyaltyService {

    private final LoyaltyAccountRepository loyaltyRepository;

    public LoyaltyService(LoyaltyAccountRepository loyaltyRepository) {
        this.loyaltyRepository = loyaltyRepository;
    }

    @Transactional
    public LoyaltyAccount getOrCreate(Long tenantId, Long userId) {
        return loyaltyRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseGet(() -> loyaltyRepository.save(new LoyaltyAccount(tenantId, userId)));
    }

    @Transactional
    public LoyaltyAccount earn(Long tenantId, Long userId, long points) {
        LoyaltyAccount account = getOrCreate(tenantId, userId);
        account.earn(points);
        return loyaltyRepository.save(account);
    }

    @Transactional
    public LoyaltyAccount redeem(Long tenantId, Long userId, long points) {
        LoyaltyAccount account = getOrCreate(tenantId, userId);
        if (!account.redeem(points)) {
            throw new IllegalArgumentException("Insufficient loyalty points");
        }
        return loyaltyRepository.save(account);
    }
}
