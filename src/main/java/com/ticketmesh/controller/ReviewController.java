package com.ticketmesh.controller;

import com.ticketmesh.dto.ReviewRequest;
import com.ticketmesh.model.Review;
import com.ticketmesh.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Customer reviews and rating analytics for products/services.
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final RequestContext requestContext;

    public ReviewController(ReviewService reviewService, RequestContext requestContext) {
        this.reviewService = reviewService;
        this.requestContext = requestContext;
    }

    @PostMapping
    public ResponseEntity<Review> submit(@RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.submit(
                requestContext.currentTenantId(), request.productId(),
                requestContext.currentUserId(), request.rating(),
                request.title(), request.comment()));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Review>> forProduct(
            @PathVariable("productId") Long productId) {
        return ResponseEntity.ok(reviewService.listForProduct(productId));
    }

    @GetMapping("/product/{productId}/average")
    public ResponseEntity<Double> average(@PathVariable("productId") Long productId) {
        return ResponseEntity.ok(reviewService.averageRating(productId));
    }
}