package com.ticketmesh.repository;

import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.model.ProviderProduct.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProviderProductRepository extends JpaRepository<ProviderProduct, Long> {

    Optional<ProviderProduct> findByIdAndTenant_Id(Long id, Long tenantId);

    List<ProviderProduct> findByProvider_Id(Long providerId);

    List<ProviderProduct> findByProvider_IdAndEnabledTrue(Long providerId);

    List<ProviderProduct> findByProductType(ProductType productType);

    List<ProviderProduct> findByTenant_IdAndEnabledTrue(Long tenantId);

    List<ProviderProduct> findByProductTypeAndTenant_IdAndEnabledTrue(
            ProductType productType, Long tenantId);

    @Query("SELECT p FROM ProviderProduct p JOIN FETCH p.provider pr " +
           "WHERE p.enabled = true " +
           "AND (LOWER(p.title) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "     OR LOWER(p.origin) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "     OR LOWER(p.destination) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "     OR LOWER(pr.name) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<ProviderProduct> searchEnabledByText(@Param("q") String query);
}
