package com.ticketmesh.service;

import com.ticketmesh.dto.RefundQuote;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.LoyaltyLedgerEntry;
import com.ticketmesh.model.Notification;
import com.ticketmesh.model.ProductOrder;
import com.ticketmesh.model.RefundOutcome;
import com.ticketmesh.model.RefundPolicyWindow;
import com.ticketmesh.model.SettlementEntry;
import com.ticketmesh.repository.ProductOrderRepository;
import com.ticketmesh.repository.RefundPolicyWindowRepository;
import com.ticketmesh.repository.SettlementRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Cancellation and refunds (spec section 26).
 *
 * <p>Two-step by design: {@link #quote} prices a cancellation without mutating
 * anything, and {@link #cancel} applies exactly that pricing. All the money maths
 * lives in {@link RefundCalculator}; this class only orchestrates persistence,
 * the wallet, the loyalty clawback and the settlement hold.
 */
@Service
public class CancellationService {

    private final ProductOrderRepository orderRepository;
    private final RefundPolicyWindowRepository windowRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final LoyaltyService loyaltyService;
    private final NotificationService notificationService;
    private final EventService eventService;
    private final CurrentUser currentUser;

    public CancellationService(ProductOrderRepository orderRepository,
                               RefundPolicyWindowRepository windowRepository,
                               SettlementRepository settlementRepository,
                               UserRepository userRepository,
                               WalletService walletService,
                               LoyaltyService loyaltyService,
                               NotificationService notificationService,
                               EventService eventService,
                               CurrentUser currentUser) {
        this.orderRepository = orderRepository;
        this.windowRepository = windowRepository;
        this.settlementRepository = settlementRepository;
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.loyaltyService = loyaltyService;
        this.notificationService = notificationService;
        this.eventService = eventService;
        this.currentUser = currentUser;
    }

    /**
     * Price a cancellation without changing anything.
     */
    @Transactional(readOnly = true)
    public RefundQuote quote(String orderRef) {
        ProductOrder order = loadOwned(orderRef);
        return price(order, Instant.now());
    }

    /**
     * Cancel an order, refunding to the wallet and clawing back loyalty points.
     */
    @Transactional
    public RefundQuote cancel(String orderRef, String reason) {
        ProductOrder order = loadOwned(orderRef);

        if (order.getStatus() == ProductOrder.Status.CANCELLED
                || order.getStatus() == ProductOrder.Status.REFUNDED) {
            throw new ConflictException("Order " + orderRef + " is already "
                    + order.getStatus().name().toLowerCase());
        }
        if (order.getStatus() == ProductOrder.Status.USED) {
            throw new ConflictException("Order " + orderRef + " has already been used and cannot be cancelled");
        }
        if (order.getStatus() == ProductOrder.Status.PENDING) {
            // Nothing was ever charged: release the hold and give the stock back.
            String oldStatus = order.getStatus().name();
            order.applyCancellation(new RefundOutcome(false, BigDecimal.ZERO, BigDecimal.ZERO,
                    reason == null || reason.isBlank() ? "Cancelled before payment" : reason, null));
            orderRepository.save(order);
            releaseInventory(order);
            return new RefundQuote(orderRef, false, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO.setScale(2), order.getCancellationReason(), null, order.getCurrencyIso());
        }

        RefundQuote q = price(order, Instant.now());
        boolean wasIssued = order.getStatus() == ProductOrder.Status.ISSUED;
        String oldStatus = order.getStatus().name();

        order.applyCancellation(new RefundOutcome(q.refundable(), q.refundAmount(), q.cancellationFee(),
                q.reason(), q.refundable() ? generateReference() : null));
        orderRepository.save(order);

        if (q.refundable()) {
            walletService.creditRefund(orderRef, q.refundAmount(), order.getTenant().getId(),
                    order.getUser().getId(), order.getCurrencyIso());
        }

        long clawedBack = loyaltyService.reverseForOrder(order.getTenant().getId(), order.getUser().getId(),
                order.getLoyaltyPointsGranted(), orderRef);
        order.setLoyaltyPointsGranted(Math.max(0, order.getLoyaltyPointsGranted() - clawedBack));
        orderRepository.save(order);

        releaseInventory(order);
        updateSettlement(orderRef, q.refundable());

        var user = userRepository.findById(order.getUser().getId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        notificationService.notify(order.getTenant().getId(), user.getId(), Notification.Channel.IN_APP,
                "Order cancelled",
                q.refundable()
                        ? "Order " + orderRef + " was cancelled and " + q.refundAmount()
                          + " " + order.getCurrencyIso() + " was returned to your wallet."
                        : "Order " + orderRef + " was cancelled. " + q.reason());

        eventService.emit("PRODUCT_ORDER", orderRef, q.refundable() ? "ORDER_REFUNDED" : "ORDER_CANCELLED",
                "{\"orderRef\":\"" + orderRef + "\",\"refund\":\"" + q.refundAmount()
                        + "\",\"fee\":\"" + q.cancellationFee() + "\",\"currency\":\"" + order.getCurrencyIso()
                        + "\",\"fromStatus\":\"" + oldStatus + "\",\"wasIssued\":" + wasIssued + "}");

        return q;
    }

    private RefundQuote price(ProductOrder order, Instant now) {
        List<RefundPolicyWindow> windows = order.getProduct() == null
                ? List.of()
                : windowRepository.findByProductId(order.getProduct().getId());
        return RefundCalculator.quote(order.getOrderRef(), order.getTotalAmount(), order.getCurrencyIso(),
                eventStart(order), now, windows);
    }

    private Instant eventStart(ProductOrder order) {
        if (order.getProduct() == null || order.getProduct().getEventDate() == null) {
            return null;
        }
        return order.getProduct().getEventDate().atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * Cancellation puts the seats back on sale; the seat map is only meaningful
     * for seat-based products, so those are released wholesale.
     */
    private void releaseInventory(ProductOrder order) {
        if (order.getProduct() == null) {
            return;
        }
        var product = order.getProduct();
        product.setAvailableQuantity(product.getAvailableQuantity() + order.getQuantity());
    }

    private void updateSettlement(String orderRef, boolean refunded) {
        for (SettlementEntry entry : settlementRepository.findByOrderRef(orderRef)) {
            if (refunded) {
                entry.markRefunded();
            } else {
                entry.hold();
            }
            settlementRepository.save(entry);
        }
    }

    private ProductOrder loadOwned(String orderRef) {
        var user = userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        ProductOrder order = orderRepository.findByOrderRef(orderRef)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderRef));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ConflictException("Order does not belong to the current user");
        }
        return order;
    }

    private String generateReference() {
        return "RF-" + System.currentTimeMillis();
    }
}
