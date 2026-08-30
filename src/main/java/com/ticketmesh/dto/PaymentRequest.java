package com.ticketmesh.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class PaymentRequest {

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "(?i)CARD|UPI|WALLET|PAYPAL|BANK",
            message = "method must be CARD, UPI, WALLET, PAYPAL or BANK")
    private String method;

    @Size(max = 16)
    private String cardNumber;

    @Size(max = 4)
    private String cardExpiry;

    @Size(max = 4)
    private String cardCvv;

    public BigDecimal getAmount() {
        return amount;
    }

    public String getMethod() {
        return method;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getCardExpiry() {
        return cardExpiry;
    }

    public String getCardCvv() {
        return cardCvv;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public void setCardExpiry(String cardExpiry) {
        this.cardExpiry = cardExpiry;
    }

    public void setCardCvv(String cardCvv) {
        this.cardCvv = cardCvv;
    }
}
