package com.ticketmesh.repository;

import com.ticketmesh.model.OrderAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderAuditLogRepository extends JpaRepository<OrderAuditLog, Long> {
    List<OrderAuditLog> findByOrderIdOrderByTimestampDesc(Long orderId);
}
