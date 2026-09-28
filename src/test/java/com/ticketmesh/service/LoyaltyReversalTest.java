package com.ticketmesh.service;

import com.ticketmesh.model.LoyaltyAccount;
import com.ticketmesh.model.LoyaltyLedgerEntry;
import com.ticketmesh.repository.LoyaltyAccountRepository;
import com.ticketmesh.repository.LoyaltyLedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoyaltyReversalTest {

    private LoyaltyAccountRepository accountRepository;
    private LoyaltyLedgerRepository ledgerRepository;
    private LoyaltyService service;

    @BeforeEach
    void setUp() {
        accountRepository = mock(LoyaltyAccountRepository.class);
        ledgerRepository = mock(LoyaltyLedgerRepository.class);
        service = new LoyaltyService(accountRepository, ledgerRepository);
    }

    private LoyaltyAccount accountWith(long points, long lifetime) {
        LoyaltyAccount a = new LoyaltyAccount(1L, 99L);
        // earn() bumps both, so simulate history then top up lifetime as needed
        a.earn(points);
        for (long i = 0; i < lifetime - points; i++) {
            a.earn(1);
        }
        return a;
    }

    @Test
    void reverse_removesPointsAndWritesReversalEntry() {
        LoyaltyAccount account = accountWith(1000, 1000);
        when(accountRepository.findByTenantIdAndUserId(1L, 99L)).thenReturn(java.util.Optional.of(account));

        long removed = service.reverseForOrder(1L, 99L, 400, "TM-1");

        assertEquals(400, removed);
        assertEquals(600, account.getPoints());

        ArgumentCaptor<LoyaltyLedgerEntry> captor = ArgumentCaptor.forClass(LoyaltyLedgerEntry.class);
        verify(ledgerRepository).save(captor.capture());
        LoyaltyLedgerEntry entry = captor.getValue();
        assertEquals(-400, entry.getPointsDelta());
        assertEquals(600, entry.getBalanceAfter());
        assertEquals(LoyaltyLedgerEntry.EntryType.REVERSAL, entry.getType());
        assertEquals("TM-1", entry.getOrderRef());
    }

    @Test
    void reverse_floorsAtZeroWhenAlreadySpent() {
        LoyaltyAccount account = accountWith(100, 100);
        when(accountRepository.findByTenantIdAndUserId(1L, 99L)).thenReturn(java.util.Optional.of(account));

        long removed = service.reverseForOrder(1L, 99L, 500, "TM-1");

        assertEquals(100, removed);
        assertEquals(0, account.getPoints());
    }

    @Test
    void reverse_doesNotDecrementLifetimePoints() {
        LoyaltyAccount account = accountWith(6000, 6000);
        when(accountRepository.findByTenantIdAndUserId(1L, 99L)).thenReturn(java.util.Optional.of(account));
        long lifetimeBefore = account.getLifetimePoints();

        service.reverseForOrder(1L, 99L, 1000, "TM-1");

        assertEquals(lifetimeBefore, account.getLifetimePoints());
    }

    @Test
    void reverse_keepsTierEarnedFromLifetimePoints() {
        LoyaltyAccount account = accountWith(6000, 6000);
        when(accountRepository.findByTenantIdAndUserId(1L, 99L)).thenReturn(java.util.Optional.of(account));
        assertEquals(LoyaltyAccount.Tier.GOLD, account.getTier());

        service.reverseForOrder(1L, 99L, 1000, "TM-1");

        assertEquals(LoyaltyAccount.Tier.GOLD, account.getTier());
    }

    @Test
    void reverse_zeroPoints_isNoOp() {
        long removed = service.reverseForOrder(1L, 99L, 0, "TM-1");

        assertEquals(0, removed);
        verify(ledgerRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void reverse_negativePoints_isNoOp() {
        assertEquals(0, service.reverseForOrder(1L, 99L, -50, "TM-1"));
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void reverse_zeroBalanceReversesNothingButStillAudits() {
        LoyaltyAccount account = accountWith(0, 0);
        when(accountRepository.findByTenantIdAndUserId(1L, 99L)).thenReturn(java.util.Optional.of(account));

        long removed = service.reverseForOrder(1L, 99L, 300, "TM-1");

        assertEquals(0, removed);
        assertTrue(account.getPoints() == 0);
        ArgumentCaptor<LoyaltyLedgerEntry> captor = ArgumentCaptor.forClass(LoyaltyLedgerEntry.class);
        verify(ledgerRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getPointsDelta());
    }
}
