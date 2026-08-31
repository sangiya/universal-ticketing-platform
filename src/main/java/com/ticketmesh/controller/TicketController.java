package com.ticketmesh.controller;

import com.ticketmesh.dto.MarketplaceTicketResponse;
import com.ticketmesh.dto.TicketResponse;
import com.ticketmesh.dto.VerificationResponse;
import com.ticketmesh.service.TicketService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable("bookingId") Long bookingId) {
        return ResponseEntity.ok(ticketService.getTicket(bookingId));
    }

    @GetMapping(value = "/booking/{bookingId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrPng(@PathVariable("bookingId") Long bookingId) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(ticketService.getQrPng(bookingId));
    }

    @GetMapping("/order/{orderRef}")
    public ResponseEntity<MarketplaceTicketResponse> getMarketplaceTicket(@PathVariable("orderRef") String orderRef) {
        return ResponseEntity.ok(ticketService.getMarketplaceTicket(orderRef));
    }

    @GetMapping(value = "/order/{orderRef}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getMarketplaceQr(@PathVariable("orderRef") String orderRef) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(ticketService.getMarketplaceQrPng(orderRef));
    }

    @GetMapping("/verify")
    public ResponseEntity<VerificationResponse> verify(@RequestParam("data") String qrData) {
        return ResponseEntity.ok(ticketService.verify(qrData));
    }
}
