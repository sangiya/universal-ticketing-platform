package com.ticketmesh.repository;

import com.ticketmesh.model.ProductOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductOrderRepository extends JpaRepository<ProductOrder, Long> {

    List<ProductOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ProductOrder> findByTenant_IdOrderByCreatedAtDesc(Long tenantId);
}
