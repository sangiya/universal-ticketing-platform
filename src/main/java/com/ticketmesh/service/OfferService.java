package com.ticketmesh.service;

import com.ticketmesh.dto.OfferResponse;
import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.repository.ProviderProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Offer / deal service. Returns time-limited special deals from the catalog.
 * An offer is a ProviderProduct marked with is_offer=true and carries the
 * original price + discount metadata inline so a second table is not needed.
 */
@Service
public class OfferService {

    private final ProviderProductRepository productRepository;

    public OfferService(ProviderProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Returns all active deals across all tenants. The dealValidUntil field is
     * checked server-side; expired deals are filtered out in the query.
     */
    @Transactional(readOnly = true)
    public List<OfferResponse> listActive() {
        return productRepository.findActiveOffers()
                .stream()
                .map(this::toOfferResponse)
                .toList();
    }

    private OfferResponse toOfferResponse(ProviderProduct p) {
        var dealType = p.getDealType() != null
                ? OfferResponse.DealType.valueOf(p.getDealType().name())
                : OfferResponse.DealType.PERCENTAGE_OFF;
        int discount = p.getDiscountPercent() != null ? p.getDiscountPercent() : 0;
        if (discount == 0 && p.getOriginalPrice() != null && p.getPrice() != null
                && p.getOriginalPrice().compareTo(p.getPrice()) > 0) {
            discount = p.getOriginalPrice()
                    .subtract(p.getPrice())
                    .multiply(java.math.BigDecimal.valueOf(100))
                    .divide(p.getOriginalPrice(), 0, java.math.RoundingMode.HALF_UP)
                    .intValue();
        }
        String tag = p.getDealTag() != null ? p.getDealTag()
                : discount > 0 ? "🏷 " + discount + "% OFF"
                : "⚡ DEAL";
        int seats = p.getDealSeats() != null ? p.getDealSeats()
                : Math.min(p.getAvailableQuantity(), 20);
        return new OfferResponse(
                p.getId(),
                p.getProvider().getCode(),
                p.getProvider().getName(),
                p.getProductType().name(),
                p.getTitle(),
                p.getOrigin(),
                p.getDestination(),
                p.getEventDate(),
                p.getPrice(),
                p.getOriginalPrice(),
                discount,
                dealType,
                tag,
                p.getDealValidUntil(),
                seats,
                p.getDealSeats() != null ? p.getDealSeats() : p.getAvailableQuantity(),
                p.getCurrencyIso(),
                p.getThumbnailUrl()
        );
    }
}
