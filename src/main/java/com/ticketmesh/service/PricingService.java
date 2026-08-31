package com.ticketmesh.service;

import com.ticketmesh.model.LoyaltyAccount;
import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.model.Promotion;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pricing & fare engine. Computes a transparent price breakdown from the base
 * price plus configurable tax rate and service fee, optionally applies a valid
 * promotion discount, and converts to the shopper's currency. Every amount stays
 * tied to its currency (multi-currency safe).
 */
@Service
public class PricingService {

    private final GlobalizationService globalizationService;
    private final PromotionService promotionService;
    private final LoyaltyService loyaltyService;

    public PricingService(GlobalizationService globalizationService,
                           PromotionService promotionService,
                           LoyaltyService loyaltyService) {
        this.globalizationService = globalizationService;
        this.promotionService = promotionService;
        this.loyaltyService = loyaltyService;
    }

    public Breakdown breakdown(ProviderProduct product, String promoCode,
                               String targetCurrency, Long tenantId) {
        return breakdown(product, promoCode, targetCurrency, tenantId, null);
    }


    /**
     * Build a full price breakdown for a product in a target currency.
     */
    public Breakdown breakdown(ProviderProduct product, String promoCode,
                                   String targetCurrency, Long tenantId, Long userId) {
        BigDecimal base = product.getBasePrice() != null
                ? product.getBasePrice()
                : product.getPrice();
        BigDecimal taxRate = product.getTaxRate() != null
                ? product.getTaxRate()
                : BigDecimal.ZERO;
        BigDecimal serviceFee = product.getServiceFee() != null
                ? product.getServiceFee()
                : BigDecimal.ZERO;

        BigDecimal tax = base.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = base.add(tax).add(serviceFee);

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = subtotal;
        String appliedCode = null;
        String promoName = null;

        // 1. Check Loyalty Tier Benefit (Permanent discount)
        if (userId != null) {
            LoyaltyAccount account = loyaltyService.getOrCreate(tenantId, userId);
            BigDecimal loyaltyBenefit = getLoyaltyDiscount(account.getTier(), subtotal);
            if (loyaltyBenefit.compareTo(BigDecimal.ZERO) > 0) {
                discount = loyaltyBenefit;
                promoName = "Loyalty " + account.getTier() + " Benefit";
            }
        }

        // 2. Promo Code (Overwrites or adds to loyalty? Usually, users pick the best one)
        if (promoCode != null && !promoCode.isBlank()) {
            Promotion promotion = promotionService.validate(tenantId, promoCode, subtotal);
            if (promotion != null) {
                BigDecimal promoDiscount = discountAmount(promotion, subtotal);
                if (promoDiscount.compareTo(discount) > 0) {
                    discount = promoDiscount;
                    promoName = promotion.getName();
                    appliedCode = promotion.getCode();
                }
            }
        }

        total = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);

        BigDecimal converted = globalizationService.convert(
                total, product.getCurrencyIso(), targetCurrency, tenantId);
        return new Breakdown(base, tax, serviceFee, discount, subtotal, total,
                product.getCurrencyIso(), targetCurrency, appliedCode, converted, promoName);
    }

    private BigDecimal getLoyaltyDiscount(LoyaltyAccount.Tier tier, BigDecimal subtotal) {
        BigDecimal rate = BigDecimal.ZERO;
        switch (tier) {
            case PLATINUM -> rate = new BigDecimal("0.10"); // 10% off
            case GOLD -> rate = new BigDecimal("0.05");     // 5% off
            case SILVER -> rate = new BigDecimal("0.02");    // 2% off
            default -> rate = BigDecimal.ZERO;
        }
        return subtotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }


    /**
     * Compute and consume a valid promotion (validation + usage increment) at
     * checkout. Returns the discount amount or zero.
     */
    public BigDecimal redeemAndDiscount(Long tenantId, String promoCode, BigDecimal subtotal) {
        Promotion promotion = promotionService.redeem(tenantId, promoCode, subtotal);
        if (promotion == null) {
            return BigDecimal.ZERO;
        }
        return discountAmount(promotion, subtotal);
    }

    public BigDecimal discountAmount(Promotion promotion, BigDecimal subtotal) {
        if (promotion.getDiscountType() == Promotion.DiscountType.PERCENT) {
            BigDecimal discount = subtotal
                    .multiply(promotion.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return discount.min(subtotal);
        }
        return promotion.getDiscountValue().min(subtotal);
    }

    /**
     * Immutable price breakdown value object.
     */
    public static final class Breakdown {
        public final BigDecimal base;
        public final BigDecimal tax;
        public final BigDecimal serviceFee;
        public final BigDecimal discount;
        public final BigDecimal subtotal;
        public final BigDecimal total;
        public final String currencyIso;
        public final String targetCurrency;
        public final String appliedPromoCode;
        public final BigDecimal convertedTotal;
        public final String promoName;

        public Breakdown(BigDecimal base, BigDecimal tax, BigDecimal serviceFee,
                         BigDecimal discount, BigDecimal subtotal, BigDecimal total,
                         String currencyIso, String targetCurrency, String appliedPromoCode,
                         BigDecimal convertedTotal, String promoName) {
            this.base = base;
            this.tax = tax;
            this.serviceFee = serviceFee;
            this.discount = discount;
            this.subtotal = subtotal;
            this.total = total;
            this.currencyIso = currencyIso;
            this.targetCurrency = targetCurrency;
            this.appliedPromoCode = appliedPromoCode;
            this.convertedTotal = convertedTotal;
            this.promoName = promoName;
        }
    }
}
