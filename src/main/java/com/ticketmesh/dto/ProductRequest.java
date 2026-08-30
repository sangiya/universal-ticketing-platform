package com.ticketmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductRequest {

    @NotBlank
    private String productType;

    @NotBlank
    @Size(max = 200)
    private String title;

    private String origin;
    private String destination;
    private LocalDateTime eventDate;

    @NotNull
    private BigDecimal price;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currencyIso;

    @PositiveOrZero
    private int availableQuantity;

    @Size(max = 1000)
    private String description;

    private String attributes;

    public String getProductType() {
        return productType;
    }

    public String getTitle() {
        return title;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrencyIso() {
        return currencyIso;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public String getDescription() {
        return description;
    }

    public String getAttributes() {
        return attributes;
    }
}
