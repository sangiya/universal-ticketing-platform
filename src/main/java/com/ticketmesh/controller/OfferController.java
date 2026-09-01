package com.ticketmesh.controller;

import com.ticketmesh.dto.OfferResponse;
import com.ticketmesh.service.OfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public listing of active offers / deals. An offer is a special deal on a
 * product (e.g. "20% off this weekend"), distinct from regular tickets and
 * distinct from promotional discount codes.
 */
@RestController
@RequestMapping("/api/offers")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @GetMapping
    public ResponseEntity<List<OfferResponse>> listActive() {
        return ResponseEntity.ok(offerService.listActive());
    }
}
