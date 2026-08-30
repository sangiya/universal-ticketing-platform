package com.ticketmesh.dto;

import java.math.BigDecimal;

public record ExchangeRateRequest(Long tenantId, String base, String target, BigDecimal rate) {
}