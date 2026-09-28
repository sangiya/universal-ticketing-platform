package com.ticketmesh.service;

import com.ticketmesh.dto.RefundQuote;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.AgentShop;
import com.ticketmesh.model.ProductOrder;
import com.ticketmesh.model.Provider;
import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.model.RefundPolicyWindow;
import com.ticketmesh.model.SettlementEntry;
import com.ticketmesh.model.Tenant;
import com.ticketmesh.model.User;
import com.ticketmesh.model.WalletTransaction;
import com.ticketmesh.repository.ProductOrderRepository;
import com.ticketmesh.repository.RefundPolicyWindowRepository;
import com.ticketmesh.repository.SettlementRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.repository.WalletTransactionRepository;
import com.ticketmesh.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancellationServiceTest {

    private ProductOrderRepository orderRepository;
    private RefundPolicyWindowRepository windowRepository;
    private SettlementRepository settlementRepository;
    private UserRepository userRepository;
    private WalletService walletService;
    private LoyaltyService loyaltyService;
    private NotificationService notificationService;
    private EventService eventService;
    private CurrentUser currentUser;
    private CancellationService service;

    private Tenant tenant;
    private Provider provider;
    private ProviderProduct product;
    private User user;
    private ProductOrder order;

    @BeforeEach
    void setUp() {
        orderRepository = mock(ProductOrderRepository.class);
        windowRepository = mock(RefundPolicyWindowRepository.class);
        settlementRepository = mock(SettlementRepository.class);
        userRepository = mock(UserRepository.class);
        walletService = mock(WalletService.class);
        loyaltyService = mock(LoyaltyService.class);
        notificationService = mock(NotificationService.class);
        eventService = mock(EventService.class);
        currentUser = mock(CurrentUser.class);

        service = new CancellationService(orderRepository, windowRepository, settlementRepository,
                userRepository, walletService, loyaltyService, notificationService, eventService, currentUser);

        tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(1L);
        provider = new Provider("TC-001", "Test Provider", mock(AgentShop.class), tenant,
                "LK", "LKR", "Asia/Colombo", "https://api.testco.local", "api-key",
                Provider.ProviderVertical.EVENT, "booking,payment");
        // Event starts in 200 hours so a 168h window qualifies.
        product = new ProviderProduct(provider, tenant, ProviderProduct.ProductType.TICKET,
                "Headline Event", "Colombo", null, LocalDateTime.now().plusDays(9),
                new BigDecimal("5000.00"), "LKR", 50, "Big event", null);
        product.setBasePrice(new BigDecimal("5000.00"));
        product.setTaxRate(BigDecimal.ZERO);
        product.setServiceFee(BigDecimal.ZERO);
        setProductId(product, 7L);

        user = mock(User.class);
        when(user.getId()).thenReturn(99L);

        when(currentUser.username()).thenReturn("customer");
        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(user));
        when(userRepository.findById(99L)).thenReturn(Optional.of(user));
        when(settlementRepository.findByOrderRef(anyString())).thenReturn(List.of());

        order = paidOrder(ProductOrder.Status.PAID);
        when(orderRepository.findByOrderRef("TM-TEST01")).thenReturn(Optional.of(order));
        when(windowRepository.findByProductId(7L)).thenReturn(List.of(
                new RefundPolicyWindow(7L, 168, new BigDecimal("90"), null, null, "7d+"),
                new RefundPolicyWindow(7L, 24, new BigDecimal("50"), null, null, "1d+")));
    }

    /**
     * Product ids are identity-generated, so set the field reflectively for tests.
     */
    private void setProductId(ProviderProduct p, Long id) {
        try {
            var field = ProviderProduct.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(p, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private ProductOrder paidOrder(ProductOrder.Status status) {
        ProductOrder o = new ProductOrder("TM-TEST01", tenant, user, product, 2,
                new BigDecimal("5000.00"), "LKR",
                new BigDecimal("10000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("10000.00"), null, status);
        o.setLoyaltyPointsGranted(1000L);
        return o;
    }

    @Test
    void quote_doesNotMutateTheOrder() {
        RefundQuote q = service.quote("TM-TEST01");

        assertTrue(q.refundable());
        assertEquals(ProductOrder.Status.PAID, order.getStatus());
        verify(orderRepository, never()).save(any());
        verify(walletService, never()).creditRefund(anyString(), any(), anyLong(), anyLong(), anyString());
    }

    @Test
    void cancel_paidOrder_refundsWalletAndClawsBackPoints() {
        RefundQuote q = service.cancel("TM-TEST01", null);

        assertTrue(q.refundable());
        assertEquals(ProductOrder.Status.REFUNDED, order.getStatus());
        assertEquals(0, order.getRefundedAmount().compareTo(new BigDecimal("9000.00")));
        assertEquals(0, order.getCancellationFee().compareTo(new BigDecimal("1000.00")));
        assertTrue(order.getRefundReference() != null && order.getRefundReference().startsWith("RF-"));
        verify(walletService).creditRefund(eq("TM-TEST01"), eq(new BigDecimal("9000.00")), eq(1L), eq(99L), eq("LKR"));
        verify(loyaltyService).reverseForOrder(1L, 99L, 1000L, "TM-TEST01");
    }

    @Test
    void cancel_paidOrder_restoresInventory() {
        product.setAvailableQuantity(10);
        service.cancel("TM-TEST01", null);
        assertEquals(12, product.getAvailableQuantity());
    }

    @Test
    void cancel_marksSettlementRefunded() {
        SettlementEntry entry = mock(SettlementEntry.class);
        when(settlementRepository.findByOrderRef("TM-TEST01")).thenReturn(List.of(entry));

        service.cancel("TM-TEST01", null);

        verify(entry).markRefunded();
        verify(settlementRepository).save(entry);
    }

    @Test
    void cancel_nonRefundable_holdsSettlementAndSkipsWallet() {
        when(windowRepository.findByProductId(7L)).thenReturn(List.of());

        RefundQuote q = service.cancel("TM-TEST01", "Customer changed plans");

        assertFalse(q.refundable());
        assertEquals(ProductOrder.Status.CANCELLED, order.getStatus());
        verify(walletService, never()).creditRefund(anyString(), any(), anyLong(), anyLong(), anyString());
    }

    @Test
    void cancel_nonRefundable_holdsSettlement() {
        SettlementEntry entry = mock(SettlementEntry.class);
        when(settlementRepository.findByOrderRef("TM-TEST01")).thenReturn(List.of(entry));
        when(windowRepository.findByProductId(7L)).thenReturn(List.of());

        service.cancel("TM-TEST01", null);

        verify(entry).hold();
    }

    @Test
    void cancel_pendingOrder_releasesHoldWithNoMoneyMovement() {
        ProductOrder pending = paidOrder(ProductOrder.Status.PENDING);
        when(orderRepository.findByOrderRef("TM-TEST01")).thenReturn(Optional.of(pending));
        product.setAvailableQuantity(8);

        RefundQuote q = service.cancel("TM-TEST01", null);

        assertEquals(ProductOrder.Status.CANCELLED, pending.getStatus());
        assertFalse(q.refundable());
        assertEquals(10, product.getAvailableQuantity());
        verify(walletService, never()).creditRefund(anyString(), any(), anyLong(), anyLong(), anyString());
        verify(loyaltyService, never()).reverseForOrder(anyLong(), anyLong(), org.mockito.ArgumentMatchers.anyLong(), anyString());
    }

    @Test
    void cancel_pendingOrder_keepsGivenReason() {
        ProductOrder pending = paidOrder(ProductOrder.Status.PENDING);
        when(orderRepository.findByOrderRef("TM-TEST01")).thenReturn(Optional.of(pending));

        service.cancel("TM-TEST01", "Booked by mistake");

        assertEquals("Booked by mistake", pending.getCancellationReason());
    }

    @Test
    void cancel_alreadyCancelled_isRejected() {
        order.applyCancellation(new com.ticketmesh.model.RefundOutcome(
                false, BigDecimal.ZERO, BigDecimal.ZERO, "done", null));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.cancel("TM-TEST01", null));
        assertTrue(ex.getMessage().contains("already cancelled"));
    }

    @Test
    void cancel_alreadyRefunded_isRejected() {
        order.applyCancellation(new com.ticketmesh.model.RefundOutcome(
                true, new BigDecimal("100.00"), BigDecimal.ZERO, "done", "RF-1"));

        assertThrows(ConflictException.class, () -> service.cancel("TM-TEST01", null));
    }

    @Test
    void cancel_usedOrder_isRejected() {
        ProductOrder used = paidOrder(ProductOrder.Status.USED);
        when(orderRepository.findByOrderRef("TM-TEST01")).thenReturn(Optional.of(used));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.cancel("TM-TEST01", null));
        assertTrue(ex.getMessage().contains("already been used"));
    }

    @Test
    void cancel_otherUsersOrder_isRejected() {
        User other = mock(User.class);
        when(other.getId()).thenReturn(1234L);
        ProductOrder theirs = new ProductOrder("TM-TEST01", tenant, other, product, 1,
                new BigDecimal("5000.00"), "LKR", new BigDecimal("5000.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("5000.00"), null, ProductOrder.Status.PAID);
        when(orderRepository.findByOrderRef("TM-TEST01")).thenReturn(Optional.of(theirs));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.cancel("TM-TEST01", null));
        assertTrue(ex.getMessage().contains("does not belong"));
    }

    @Test
    void cancel_unknownOrder_throwsNotFound() {
        when(orderRepository.findByOrderRef("TM-NOPE")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.cancel("TM-NOPE", null));
    }

    @Test
    void quote_unknownOrder_throwsNotFound() {
        when(orderRepository.findByOrderRef("TM-NOPE")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.quote("TM-NOPE"));
    }

    @Test
    void cancel_issuedOrder_isAllowed() {
        ProductOrder issued = paidOrder(ProductOrder.Status.ISSUED);
        when(orderRepository.findByOrderRef("TM-TEST01")).thenReturn(Optional.of(issued));

        RefundQuote q = service.cancel("TM-TEST01", null);

        assertTrue(q.refundable());
        assertEquals(ProductOrder.Status.REFUNDED, issued.getStatus());
    }

    @Test
    void cancel_settlementSavedAndEmitsEvent() {
        service.cancel("TM-TEST01", null);

        verify(eventService).emit(eq("PRODUCT_ORDER"), eq("TM-TEST01"), eq("ORDER_REFUNDED"), anyString());
        verify(notificationService).notify(eq(1L), eq(99L), any(), anyString(), anyString());
    }

    @Test
    void walletRefundUsesOrderCurrency() {
        service.cancel("TM-TEST01", null);

        verify(walletService).creditRefund(anyString(), any(), anyLong(), anyLong(), eq("LKR"));
    }
}
