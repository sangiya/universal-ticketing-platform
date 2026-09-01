package com.ticketmesh.service;

import com.ticketmesh.model.AgentShop;
import com.ticketmesh.model.Notification;
import com.ticketmesh.model.OutboxEvent;
import com.ticketmesh.model.ProductOrder;
import com.ticketmesh.model.Provider;
import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.model.Tenant;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.OrderAuditLogRepository;
import com.ticketmesh.repository.ProductOrderRepository;
import com.ticketmesh.repository.ProviderProductRepository;
import com.ticketmesh.repository.SettlementRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductOrderServiceTest {

    private ProductOrderRepository orderRepository;
    private ProviderProductRepository productRepository;
    private UserRepository userRepository;
    private PricingService pricingService;
    private PromotionService promotionService;
    private LoyaltyService loyaltyService;
    private NotificationService notificationService;
    private EventService eventService;
    private OrderAuditLogRepository auditRepository;
    private SettlementRepository settlementRepository;
    private CurrentUser currentUser;
    private ProductOrderService orderService;

    private Tenant tenant;
    private Provider provider;
    private ProviderProduct product;
    private User user;

    @BeforeEach
    void setUp() {
        orderRepository = mock(ProductOrderRepository.class);
        productRepository = mock(ProviderProductRepository.class);
        userRepository = mock(UserRepository.class);
        pricingService = mock(PricingService.class);
        promotionService = mock(PromotionService.class);
        loyaltyService = mock(LoyaltyService.class);
        notificationService = mock(NotificationService.class);
        eventService = mock(EventService.class);
        auditRepository = mock(OrderAuditLogRepository.class);
        settlementRepository = mock(SettlementRepository.class);
        currentUser = mock(CurrentUser.class);

        orderService = new ProductOrderService(
                orderRepository, productRepository, userRepository, pricingService,
                promotionService, loyaltyService, notificationService, eventService,
                auditRepository, settlementRepository, currentUser);

        tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(1L);
        provider = new Provider("TC-001", "Test Provider", mock(AgentShop.class), tenant,
                "LK", "LKR", "Asia/Colombo", "https://api.testco.local", "api-key",
                Provider.ProviderVertical.TRAIN, "booking,payment");
        product = new ProviderProduct(provider, tenant, ProviderProduct.ProductType.SERVICE,
                "Airport Transfer", "Colombo", "Airport", LocalDateTime.now().plusDays(2),
                new BigDecimal("5000.00"), "LKR", 10, "Door-to-door transfer", null);
        product.setBasePrice(new BigDecimal("5000.00"));
        product.setTaxRate(new BigDecimal("0.10"));
        product.setServiceFee(new BigDecimal("100.00"));
        user = mock(User.class);
        when(user.getId()).thenReturn(99L);
        when(user.getTenantId()).thenReturn(null);

        when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(user));
    }

    @Test
    void create_ordersProduct_withFullBreakdown() {
        PricingService.Breakdown breakdown = new PricingService.Breakdown(
                new BigDecimal("5000.00"), new BigDecimal("500.00"), new BigDecimal("100.00"),
                BigDecimal.ZERO, new BigDecimal("5600.00"), new BigDecimal("5600.00"),
                "LKR", "LKR", null, new BigDecimal("5600.00"), null);
        when(pricingService.breakdown(eq(product), isNull(), eq("LKR"), anyLong(), eq(99L)))
                .thenReturn(breakdown);
        when(pricingService.redeemAndDiscount(anyLong(), isNull(),
                any(BigDecimal.class), any())).thenReturn(BigDecimal.ZERO);

        ProductOrder order = orderService.create(1L, 1L, 2, null);

        assertNotNull(order.getOrderRef());
        assertEquals("Airport Transfer", order.getProductTitle());
        assertEquals("Test Provider", order.getProviderName());
        assertEquals(2, order.getQuantity());
        assertEquals(0, new BigDecimal("5600.00").compareTo(order.getTotalAmount()));
        assertEquals(ProductOrder.Status.PAID, order.getStatus());
        verify(orderRepository).save(any(ProductOrder.class));
        verify(productRepository).save(product);
        assertEquals(8, product.getAvailableQuantity());
        verify(eventService).emit(eq("PRODUCT_ORDER"), any(String.class),
                eq("ORDER_CREATED"), any(String.class));
        verify(loyaltyService).earnWithRef(anyLong(), anyLong(), anyLong(), any(String.class),
                any(com.ticketmesh.model.LoyaltyLedgerEntry.EntryType.class), any(String.class));
    }

    @Test
    void create_appliesPromotionDiscount() {
        PricingService.Breakdown breakdown = new PricingService.Breakdown(
                new BigDecimal("5000.00"), new BigDecimal("500.00"), new BigDecimal("100.00"),
                BigDecimal.ZERO, new BigDecimal("5600.00"), new BigDecimal("5600.00"),
                "LKR", "LKR", "WELCOME10", new BigDecimal("5600.00"), "Welcome 10%");
        when(pricingService.breakdown(eq(product), eq("WELCOME10"), eq("LKR"), anyLong(), eq(99L)))
                .thenReturn(breakdown);
        when(pricingService.redeemAndDiscount(anyLong(), eq("WELCOME10"),
                any(BigDecimal.class), any())).thenReturn(new BigDecimal("560.00"));

        ProductOrder order = orderService.create(1L, 1L, 1, "WELCOME10");

        assertEquals("WELCOME10", order.getPromoCode());
        assertEquals(0, new BigDecimal("5040.00").compareTo(order.getTotalAmount()));
    }

    @Test
    void create_rejectsUnknownProduct() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(com.ticketmesh.exception.NotFoundException.class,
                () -> orderService.create(1L, 99L, 1, null));
        verify(orderRepository, never()).save(any(ProductOrder.class));
    }

    @Test
    void create_rejectsInsufficientInventory() {
        product.setAvailableQuantity(1);

        assertThrows(RuntimeException.class, () -> orderService.create(1L, 1L, 5, null));
        verify(orderRepository, never()).save(any(ProductOrder.class));
    }

    @Test
    void create_rejectsDisabledProduct() {
        product.setEnabled(false);

        assertThrows(RuntimeException.class, () -> orderService.create(1L, 1L, 1, null));
        verify(orderRepository, never()).save(any(ProductOrder.class));
    }
}