package com.ticketmesh.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class FraudCheckRequest {

    @NotNull
    private Long tenantId;

    private String actorUsername;

    @NotNull
    private String subjectType;

    @NotNull
    private String subjectRef;

    @NotNull
    private BigDecimal amount;

    @PositiveOrZero
    private int quantity;

    @PositiveOrZero
    private int attemptsInWindow;

    @PositiveOrZero
    private int distinctCardsInWindow;

    private boolean adminOverride;

    public Long getTenantId() {
        return tenantId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public String getSubjectRef() {
        return subjectRef;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getAttemptsInWindow() {
        return attemptsInWindow;
    }

    public int getDistinctCardsInWindow() {
        return distinctCardsInWindow;
    }

    public boolean isAdminOverride() {
        return adminOverride;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public void setSubjectRef(String subjectRef) {
        this.subjectRef = subjectRef;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setAttemptsInWindow(int attemptsInWindow) {
        this.attemptsInWindow = attemptsInWindow;
    }

    public void setDistinctCardsInWindow(int distinctCardsInWindow) {
        this.distinctCardsInWindow = distinctCardsInWindow;
    }

    public void setAdminOverride(boolean adminOverride) {
        this.adminOverride = adminOverride;
    }
}
