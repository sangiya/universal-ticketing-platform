package com.ticketmesh.service;

import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.Notification;
import com.ticketmesh.model.OrderAuditLog;
import com.ticketmesh.model.ProductOrder;
import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.model.SettlementEntry;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.OrderAuditLogRepository;
import com.ticketmesh.repository.ProductOrderRepository;
import com.ticketmesh.repository.ProviderProductRepository;
import com.ticketmesh.repository.SettlementRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ProductOrderService {

    private final ProductOrderRepository orderRepository;
    private final ProviderProductRepository productRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;
    private final PromotionService promotionService;
    private final LoyaltyService loyaltyService;
    private final NotificationService notificationService;
    private final EventService eventService;
    private final OrderAuditLogRepository auditRepository;
    private final SettlementRepository settlementRepository;
    private final CurrentUser currentUser;

    public ProductOrderService(ProductOrderRepository orderRepository,
                                ProviderProductRepository productRepository,
                                UserRepository userRepository,
                                PricingService pricingService,
                                PromotionService promotionService,
                                LoyaltyService loyaltyService,
                                NotificationService notificationService,
                                EventService eventService,
                                OrderAuditLogRepository auditRepository,
                                SettlementRepository settlementRepository,
                                CurrentUser currentUser) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.pricingService = pricingService;
        this.promotionService = promotionService;
        this.loyaltyService = loyaltyService;
        this.notificationService = notificationService;
        this.eventService = eventService;
        this.auditRepository = auditRepository;
        this.settlementRepository = settlementRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public ProductOrder create(Long tenantId, Long productId, int quantity, String promoCode) {
        return createInternal(tenantId, productId, quantity, promoCode, ProductOrder.Status.PAID);
    }

    @Transactional
    public ProductOrder checkout(Long tenantId, Long productId, int quantity, String promoCode) {
        return createInternal(tenantId, productId, quantity, promoCode, ProductOrder.Status.PENDING);
    }

    @Transactional
    public ProductOrder pay(String orderRef) {
        User user = userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        ProductOrder order = orderRepository.findByOrderRef(orderRef)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderRef));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ConflictException("Order does not belong to the current user");
        }
        if (order.getStatus() == ProductOrder.Status.CANCELLED
                || order.getStatus() == ProductOrder.Status.REFUNDED) {
            throw new ConflictException("Order " + orderRef + " is " + order.getStatus().name().toLowerCase());
        }
        if (order.getStatus() == ProductOrder.Status.PAID || order.getStatus() == ProductOrder.Status.ISSUED) {
            return order;
        }
        String oldStatus = order.getStatus().name();
        order.setStatus(ProductOrder.Status.PAID);
        order.setPaidAt(Instant.now());
        orderRepository.save(order);
        auditRepository.save(new OrderAuditLog(order, oldStatus, "PAID", "Payment verified", currentUser.username()));
        loyaltyService.earnWithRef(order.getTenant().getId(), user.getId(),
                order.getTotalAmount().longValue() / 10, order.getOrderRef(),
                com.ticketmesh.model.LoyaltyLedgerEntry.EntryType.ACCRUAL, "Order " + order.getOrderRef() + " paid");
        createSettlement(order);
        notificationService.notify(order.getTenant().getId(), user.getId(),
                Notification.Channel.IN_APP, "Payment received",
                "Your order " + order.getOrderRef() + " is now paid and processing.");
        eventService.emit("PRODUCT_ORDER", order.getOrderRef(), "ORDER_PAID",
                "{\"orderRef\":\"" + order.getOrderRef() + "\",\"total\":\""
                        + order.getTotalAmount() + "\",\"currency\":\"" + order.getCurrencyIso() + "\"}");
        return order;
    }

    @Transactional
    public void issueTicket(String orderRef) {
        ProductOrder order = orderRepository.findByOrderRef(orderRef)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (order.getStatus() != ProductOrder.Status.PAID) {
            throw new ConflictException("Order must be PAID before issuance");
        }
        String oldStatus = order.getStatus().name();
        order.setStatus(ProductOrder.Status.ISSUED);
        orderRepository.save(order);
        auditRepository.save(new OrderAuditLog(order, oldStatus, "ISSUED", "Ticket generated and delivered", "SYSTEM"));
    }

    private ProductOrder createInternal(Long tenantId, Long productId, int quantity, String promoCode,
                                       ProductOrder.Status status) {
        ProviderProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));
        if (!product.isEnabled() || !product.getTenant().getId().equals(tenantId)) {
            throw new ConflictException("Product not available in this tenant");
        }
        if (quantity < 1) {
            throw new ConflictException("Quantity must be at least 1");
        }
        if (product.getAvailableQuantity() < quantity) {
            throw new ConflictException(
                    "Insufficient inventory (requested " + quantity + ", available "
                            + product.getAvailableQuantity() + ")");
        }
        User user = userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        PricingService.Breakdown b = pricingService.breakdown(
                product, promoCode, product.getCurrencyIso(), tenantId, user.getId());
        BigDecimal unitPrice = product.getPrice();
        String pType = product.getProductType() != null ? product.getProductType().name() : null;
        BigDecimal discount = promoCode != null && !promoCode.isBlank()
                ? pricingService.redeemAndDiscount(tenantId, promoCode, b.subtotal, pType)
                : BigDecimal.ZERO;
        BigDecimal total = b.subtotal.subtract(discount);
        ProductOrder order = new ProductOrder(
                generateRef(), product.getTenant(), user, product, quantity, unitPrice,
                product.getCurrencyIso(), b.base.multiply(BigDecimal.valueOf(quantity)),
                b.tax.multiply(BigDecimal.valueOf(quantity)),
                b.serviceFee.multiply(BigDecimal.valueOf(quantity)),
                discount, total, promoCode, status);
        orderRepository.save(order);
        product.setAvailableQuantity(product.getAvailableQuantity() - quantity);
        productRepository.save(product);
        auditRepository.save(new OrderAuditLog(order, "NONE", status.name(), "Order created", currentUser.username()));
        if (status == ProductOrder.Status.PAID) {
            loyaltyService.earnWithRef(tenantId, user.getId(), total.longValue() / 10, order.getOrderRef(),
                    com.ticketmesh.model.LoyaltyLedgerEntry.EntryType.ACCRUAL, "Order " + order.getOrderRef() + " created paid");
            createSettlement(order);
        }
        String eventType = status == ProductOrder.Status.PENDING ? "ORDER_PENDING" : "ORDER_CREATED";
        eventService.emit("PRODUCT_ORDER", order.getOrderRef(), eventType,
                "{\"orderRef\":\"" + order.getOrderRef() + "\",\"total\":\""
                        + total + "\",\"currency\":\"" + product.getCurrencyIso() + "\"}");
        return order;
    }

    @Transactional(readOnly = true)
    public List<ProductOrder> mine() {
        User user = userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public Optional<ProductOrder> findMine(String orderRef) {
        User user = userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        return orderRepository.findByOrderRef(orderRef)
                .filter(o -> o.getUser().getId().equals(user.getId()));
    }

    @Transactional(readOnly = true)
    public List<ProductOrder> byTenant(Long tenantId) {
        return orderRepository.findByTenant_IdOrderByCreatedAtDesc(tenantId);
    }

    private void createSettlement(ProductOrder order) {
        BigDecimal gross = order.getBaseAmount().add(order.getTaxAmount()).add(order.getServiceFee());
        BigDecimal platformFee = order.getServiceFee().setScale(2, RoundingMode.HALF_UP);
        // 3% commission on base as platform commission (example)
        BigDecimal commission = order.getBaseAmount().multiply(new BigDecimal("0.03")).setScale(2, RoundingMode.HALF_UP);
        platformFee = platformFee.add(commission);
        BigDecimal net = order.getTotalAmount().subtract(platformFee).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        SettlementEntry entry = new SettlementEntry(
                order.getTenant().getId(), order.getOrderRef(),
                order.getProduct() != null ? order.getProduct().getProvider().getId() : null,
                order.getProductType(), gross, platformFee, net, order.getCurrencyIso());
        settlementRepository.save(entry);
    }

    private String generateRef() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("TM-");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }
}
