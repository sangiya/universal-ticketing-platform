package com.ticketmesh.service;

import com.ticketmesh.dto.WalletResponse;
import com.ticketmesh.dto.WalletTopUpRequest;
import com.ticketmesh.model.Wallet;
import com.ticketmesh.model.WalletTransaction;
import com.ticketmesh.repository.WalletRepository;
import com.ticketmesh.repository.WalletTransactionRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository txRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public WalletService(WalletRepository walletRepository,
                         WalletTransactionRepository txRepository,
                         UserRepository userRepository,
                         CurrentUser currentUser) {
        this.walletRepository = walletRepository;
        this.txRepository = txRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public Wallet getOrCreate(Long tenantId, Long userId, String currency) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(new Wallet(tenantId, userId, currency)));
    }

    @Transactional(readOnly = true)
    public WalletResponse getMyWallet() {
        var user = userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        var w = getOrCreate(user.getTenantId() != null ? user.getTenantId() : 1L, user.getId(), "LKR");
        return new WalletResponse(w.getId(), w.getBalance(), w.getCurrencyIso(), w.getUpdatedAt());
    }

    @Transactional
    public WalletResponse topUp(WalletTopUpRequest req) {
        var user = userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        var w = getOrCreate(user.getTenantId() != null ? user.getTenantId() : 1L, user.getId(), req.currency());
        BigDecimal amt = req.amount();
        if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) throw new ConflictException("Amount must be positive");
        w.credit(amt);
        walletRepository.save(w);
        txRepository.save(new WalletTransaction(w.getId(), amt, WalletTransaction.Type.CREDIT, "Top-up", null));
        return new WalletResponse(w.getId(), w.getBalance(), w.getCurrencyIso(), w.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public List<WalletTransaction> history() {
        var user = userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        var w = getOrCreate(user.getTenantId() != null ? user.getTenantId() : 1L, user.getId(), "LKR");
        return txRepository.findByWalletIdOrderByCreatedAtDesc(w.getId());
    }

    @Transactional
    public void debitForOrder(String orderRef, BigDecimal amount, Long tenantId, Long userId) {
        var w = getOrCreate(tenantId, userId, "LKR");
        if (!w.debit(amount)) throw new ConflictException("Insufficient wallet balance");
        walletRepository.save(w);
        txRepository.save(new WalletTransaction(w.getId(), amount.negate(), WalletTransaction.Type.DEBIT, "Order "+orderRef, orderRef));
    }
}
