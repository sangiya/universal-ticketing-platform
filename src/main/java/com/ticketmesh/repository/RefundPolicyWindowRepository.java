package com.ticketmesh.repository;

import com.ticketmesh.model.RefundPolicyWindow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundPolicyWindowRepository extends JpaRepository<RefundPolicyWindow, Long> {

    List<RefundPolicyWindow> findByProductId(Long productId);
}
