package com.ticketmesh.repository;

import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.model.ProviderProduct.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderProductRepository extends JpaRepository<ProviderProduct, Long> {

    List<ProviderProduct> findByProvider_Id(Long providerId);

    List<ProviderProduct> findByProvider_IdAndEnabledTrue(Long providerId);

    List<ProviderProduct> findByProductType(ProductType productType);

    List<ProviderProduct> findByTenant_IdAndEnabledTrue(Long tenantId);

    List<ProviderProduct> findByProductTypeAndTenant_IdAndEnabledTrue(
            ProductType productType, Long tenantId);
}
