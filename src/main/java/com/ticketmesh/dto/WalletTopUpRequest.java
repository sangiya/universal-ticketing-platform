package com.ticketmesh.dto;

import java.math.BigDecimal;

public record WalletTopUpRequest(BigDecimal amount, String currency) {}
