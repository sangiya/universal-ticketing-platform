package com.ticketmesh.service;

import com.ticketmesh.model.LoyaltyAccount;
import com.ticketmesh.model.LoyaltyLedgerEntry;
import com.ticketmesh.repository.LoyaltyAccountRepository;
import com.ticketmesh.repository.LoyaltyLedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer loyalty: earn points on bookings, progress through tiers.
 */
@Service
public class LoyaltyService {

    private final LoyaltyAccountRepository loyaltyRepository;
    private final LoyaltyLedgerRepository ledgerRepository;

    public LoyaltyService(LoyaltyAccountRepository loyaltyRepository,
                          LoyaltyLedgerRepository ledgerRepository) {
        this.loyaltyRepository = loyaltyRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional
    public LoyaltyAccount getOrCreate(Long tenantId, Long userId) {
        return loyaltyRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseGet(() -> loyaltyRepository.save(new LoyaltyAccount(tenantId, userId)));
    }

    @Transactional
    public LoyaltyAccount earn(Long tenantId, Long userId, long points) {
        return earnWithRef(tenantId, userId, points, null, LoyaltyLedgerEntry.EntryType.ACCRUAL, "Order accrual");
    }

    @Transactional
    public LoyaltyAccount earnWithRef(Long tenantId, Long userId, long points, String orderRef,
                                      LoyaltyLedgerEntry.EntryType type, String reason) {
        LoyaltyAccount account = getOrCreate(tenantId, userId);
        account.earn(points);
        loyaltyRepository.save(account);
        ledgerRepository.save(new LoyaltyLedgerEntry(tenantId, userId, points, account.getPoints(), type, reason, orderRef));
        return account;
    }

    @Transactional
    public LoyaltyAccount redeem(Long tenantId, Long userId, long points) {
        LoyaltyAccount account = getOrCreate(tenantId, userId);
        if (!account.redeem(points)) {
            throw new IllegalArgumentException("Insufficient loyalty points");
        }
        loyaltyRepository.save(account);
        ledgerRepository.save(new LoyaltyLedgerEntry(tenantId, userId, -points, account.getPoints(),
                LoyaltyLedgerEntry.EntryType.REDEMPTION, "Redeemed at checkout", null));
        return account;
    }

    /**
     * Claw back the points an order awarded when it is cancelled or refunded
     * (spec section 26). Floors at zero if the balance was already spent.
     *
     * @return points actually removed
     */
    @Transactional
    public long reverseForOrder(Long tenantId, Long userId, long points, String orderRef) {
        if (points <= 0) {
            return 0;
        }
        LoyaltyAccount account = getOrCreate(tenantId, userId);
        long removed = account.reverse(points);
        loyaltyRepository.save(account);
        ledgerRepository.save(new LoyaltyLedgerEntry(tenantId, userId, -removed, account.getPoints(),
                LoyaltyLedgerEntry.EntryType.REVERSAL,
                "Clawed back for cancelled order " + orderRef, orderRef));
        return removed;
    }
}
