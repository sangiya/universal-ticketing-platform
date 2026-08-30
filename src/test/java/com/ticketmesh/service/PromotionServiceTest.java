package com.ticketmesh.service;

import com.ticketmesh.model.Promotion;
import com.ticketmesh.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromotionServiceTest {

    private PromotionRepository promotionRepository;
    private PromotionService promotionService;

    @BeforeEach
    void setUp() {
        promotionRepository = mock(PromotionRepository.class);
        promotionService = new PromotionService(promotionRepository);
        when(promotionRepository.save(any(Promotion.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_persistsVoucherKind() {
        Promotion promotion = createWithKind(Promotion.DiscountType.VOUCHER, Promotion.Kind.VOUCHER);
        assertEquals(Promotion.Kind.VOUCHER, promotion.getKind());
        assertEquals(Promotion.DiscountType.VOUCHER, promotion.getDiscountType());
    }

    @Test
    void create_persistsOfferKind() {
        Promotion promotion = createWithKind(Promotion.DiscountType.OFFER, Promotion.Kind.OFFER);
        assertEquals(Promotion.Kind.OFFER, promotion.getKind());
        assertEquals(Promotion.DiscountType.OFFER, promotion.getDiscountType());
    }

    @Test
    void create_defaultsToPromoKind() {
        when(promotionRepository.findByTenantIdAndCode(eq(1L), eq("PROMO1")))
                .thenReturn(Optional.empty());
        Promotion promotion = promotionService.create(
                1L, "PROMO1", "Launch", Promotion.DiscountType.FLAT,
                new BigDecimal("100"), null, null, null, null, "travel");
        assertEquals(Promotion.Kind.PROMO, promotion.getKind());
    }

    private Promotion createWithKind(Promotion.DiscountType type, Promotion.Kind kind) {
        when(promotionRepository.findByTenantIdAndCode(eq(1L), eq("CODE1")))
                .thenReturn(Optional.empty());
        return promotionService.create(
                1L, "CODE1", "Offer", type, new BigDecimal("500"),
                null, null, null, null, "travel", kind);
    }
}
