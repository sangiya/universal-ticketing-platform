package com.ticketmesh.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletResponse(Long id, BigDecimal balance, String currencyIso, Instant updatedAt) {}
