package com.ticketmesh.service;

import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.Notification;
import com.ticketmesh.model.ProductOrder;
import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.ProductOrderRepository;
import com.ticketmesh.repository.ProviderProductRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Universal marketplace order flow. A customer buys any provider product
 * through one engine: validates inventory, computes a full price breakdown
 * (base + tax + service fee + promotion), decrements inventory, issues the
 * order, awards loyalty points, sends a notification and emits a domain event.
 */
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
    private final CurrentUser currentUser;

    public ProductOrderService(ProductOrderRepository orderRepository,
                               ProviderProductRepository productRepository,
                               UserRepository userRepository,
                               PricingService pricingService,
                               PromotionService promotionService,
                               LoyaltyService loyaltyService,
                               NotificationService notificationService,
                               EventService eventService,
                               CurrentUser currentUser) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.pricingService = pricingService;
        this.promotionService = promotionService;
        this.loyaltyService = loyaltyService;
        this.notificationService = notificationService;
        this.eventService = eventService;
        this.currentUser = currentUser;
    }

    @Transactional
    public ProductOrder create(Long tenantId, Long productId, int quantity, String promoCode) {
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
                product, promoCode, product.getCurrencyIso(), tenantId);

        BigDecimal unitPrice = product.getPrice();

        // Consume promotion usage at checkout.
        BigDecimal discount = promoCode != null && !promoCode.isBlank()
                ? pricingService.redeemAndDiscount(tenantId, promoCode, b.subtotal)
                : BigDecimal.ZERO;
        BigDecimal total = b.subtotal.subtract(discount);

        ProductOrder order = new ProductOrder(
                generateRef(), product.getTenant(), user, product, quantity, unitPrice,
                product.getCurrencyIso(), b.base.multiply(BigDecimal.valueOf(quantity)),
                b.tax.multiply(BigDecimal.valueOf(quantity)),
                b.serviceFee.multiply(BigDecimal.valueOf(quantity)),
                discount, total, promoCode);
        orderRepository.save(order);

        product.setAvailableQuantity(product.getAvailableQuantity() - quantity);
        productRepository.save(product);

        loyaltyService.earn(tenantId, user.getId(), total.longValue() / 10);

        notificationService.notify(tenantId, user.getId(), Notification.Channel.IN_APP,
                "Order confirmed", "Your order " + order.getOrderRef()
                        + " for " + product.getTitle() + " is confirmed.");

        eventService.emit("PRODUCT_ORDER", order.getOrderRef(), "ORDER_CREATED",
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
    public List<ProductOrder> byTenant(Long tenantId) {
        return orderRepository.findByTenant_IdOrderByCreatedAtDesc(tenantId);
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
