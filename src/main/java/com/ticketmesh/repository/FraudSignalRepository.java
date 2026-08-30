package com.ticketmesh.repository;

import com.ticketmesh.model.FraudSignal;
import com.ticketmesh.model.FraudSignal.Risk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FraudSignalRepository extends JpaRepository<FraudSignal, Long> {

    List<FraudSignal> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<FraudSignal> findByRiskOrderByCreatedAtDesc(Risk risk);

    long countByRisk(Risk risk);
}
