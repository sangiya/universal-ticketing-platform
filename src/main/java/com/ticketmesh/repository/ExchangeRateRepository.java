package com.ticketmesh.repository;

import com.ticketmesh.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByBaseCurrencyAndTargetCurrency(String base, String target);

    Optional<ExchangeRate> findByTenantIdAndBaseCurrencyAndTargetCurrency(
            Long tenantId, String base, String target);

    List<ExchangeRate> findByTenantIdOrTenantIdIsNull(Long tenantId);
}
