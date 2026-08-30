package com.ticketmesh.repository;

import com.ticketmesh.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);
}
