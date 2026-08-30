package com.ticketmesh.service;

import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.model.Review;
import com.ticketmesh.repository.ProviderProductRepository;
import com.ticketmesh.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Customer reviews of products/services, supporting rating analytics.
 */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProviderProductRepository productRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         ProviderProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Review submit(Long tenantId, Long productId, Long userId, int rating,
                         String title, String comment) {
        if (rating < 1 || rating > 5) {
            throw new ConflictException("Rating must be between 1 and 5");
        }
        requireProduct(tenantId, productId);
        return reviewRepository.save(
                new Review(tenantId, productId, userId, rating, title, comment));
    }

    @Transactional(readOnly = true)
    public List<Review> listForProduct(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    @Transactional(readOnly = true)
    public List<Review> listByTenant(Long tenantId) {
        return reviewRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public double averageRating(Long productId) {
        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
    }

    private void requireProduct(Long tenantId, Long productId) {
        productRepository.findByIdAndTenant_Id(productId, tenantId)
                .orElseThrow(() -> new ConflictException(
                        "Product not found in this tenant: " + productId));
    }
}
