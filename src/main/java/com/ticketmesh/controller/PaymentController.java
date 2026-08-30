package com.ticketmesh.controller;

import com.ticketmesh.dto.PaymentRequest;
import com.ticketmesh.dto.PaymentResponse;
import com.ticketmesh.dto.PaymentStatusResponse;
import com.ticketmesh.dto.SettleRequest;
import com.ticketmesh.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> initiate(
            @PathVariable("bookingId") Long bookingId,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.initiate(bookingId, request));
    }

    @PostMapping("/{paymentId}/settle")
    public ResponseEntity<PaymentResponse> settle(
            @PathVariable("paymentId") Long paymentId,
            @Valid @RequestBody SettleRequest request) {
        return ResponseEntity.ok(paymentService.settle(paymentId, request));
    }

    @GetMapping("/booking/{bookingId}/status")
    public ResponseEntity<PaymentStatusResponse> status(
            @PathVariable("bookingId") Long bookingId) {
        return ResponseEntity.ok(paymentService.status(bookingId));
    }
}
