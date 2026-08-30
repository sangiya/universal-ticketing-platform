package com.ticketmesh.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PromotionRequest(Long tenantId, String code, String name, String discountType,
                               BigDecimal discountValue, BigDecimal minPurchase, Instant startsAt,
                               Instant endsAt, Integer maxUses, String domains, String kind) {
}
