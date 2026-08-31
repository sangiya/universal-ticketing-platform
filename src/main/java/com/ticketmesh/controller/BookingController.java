package com.ticketmesh.controller;

import com.ticketmesh.dto.BookingResponse;
import com.ticketmesh.dto.CreateBookingRequest;
import com.ticketmesh.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> listMine() {
        return ResponseEntity.ok(bookingService.listMine());
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookingResponse>> searchMine(
            @org.springframework.web.bind.annotation.RequestParam(value = "q", required = false) String q) {
        return ResponseEntity.ok(bookingService.searchMine(q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(bookingService.getMine(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(@PathVariable("id") Long id) {
        return ResponseEntity.ok(bookingService.cancel(id));
    }
}
