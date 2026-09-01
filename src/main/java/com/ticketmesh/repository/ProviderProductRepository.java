package com.ticketmesh.repository;

import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.model.ProviderProduct.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    /** All enabled products across tenants (browseable marketplace). */
    List<ProviderProduct> findByEnabledTrue();

    /** Enabled products of a given type across all tenants. */
    List<ProviderProduct> findByProductTypeAndEnabledTrue(ProductType productType);

    /** All enabled regular tickets (is_offer = false) — used by Tickets section. */
    @Query("SELECT p FROM ProviderProduct p JOIN FETCH p.provider " +
           "WHERE p.enabled = true AND (p.isOffer = false OR p.isOffer IS NULL) " +
           "ORDER BY p.createdAt DESC")
    List<ProviderProduct> findRegularTickets();

    /** All enabled offers / deals (is_offer = true, deal still valid) — used by Offers section. */
    @Query("SELECT p FROM ProviderProduct p JOIN FETCH p.provider " +
           "WHERE p.enabled = true AND p.isOffer = true " +
           "AND (p.dealValidUntil IS NULL OR p.dealValidUntil > CURRENT_TIMESTAMP) " +
           "ORDER BY p.discountPercent DESC NULLS LAST, p.createdAt DESC")
    List<ProviderProduct> findActiveOffers();

    long countByEnabledTrue();

    /**
     * Free-text search across product title, origin, destination, description
     * and provider name. Used by the universal search console.
     */
    @Query("SELECT p FROM ProviderProduct p JOIN FETCH p.provider pr " +
           "WHERE p.enabled = true " +
           "AND (LOWER(p.title) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "     OR LOWER(p.origin) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "     OR LOWER(p.destination) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "     OR LOWER(pr.name) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<ProviderProduct> searchEnabledByText(@Param("q") String query);

    /**
     * Universal search with optional filters. Any of the parameters can be null
     * to skip that filter.
     */
    @Query("SELECT p FROM ProviderProduct p JOIN FETCH p.provider pr " +
           "WHERE p.enabled = true " +
           "AND (:tenantId IS NULL OR p.tenant.id = :tenantId) " +
           "AND (:productType IS NULL OR p.productType = :productType) " +
           "AND (:q IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "     OR LOWER(p.origin) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "     OR LOWER(p.destination) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "     OR LOWER(pr.name) LIKE LOWER(CONCAT('%',:q,'%'))) " +
           "AND (:origin IS NULL OR LOWER(p.origin) LIKE LOWER(CONCAT('%',:origin,'%'))) " +
           "AND (:destination IS NULL OR LOWER(p.destination) LIKE LOWER(CONCAT('%',:destination,'%'))) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:dateFrom IS NULL OR p.eventDate >= :dateFrom) " +
           "AND (:dateTo IS NULL OR p.eventDate <= :dateTo) " +
           "ORDER BY p.price ASC")
    List<ProviderProduct> universalSearch(
            @Param("tenantId") Long tenantId,
            @Param("productType") ProductType productType,
            @Param("q") String q,
            @Param("origin") String origin,
            @Param("destination") String destination,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo);

    /**
     * Count products by product type — used for vertical dashboards and the
     * "Browse by category" tabs.
     */
    long countByProductType(ProductType productType);

    /**
     * Top products by popularity (orders) for a given product type.
     */
    @Query("SELECT p FROM ProviderProduct p " +
           "WHERE p.enabled = true " +
           "AND p.availableQuantity > 0 " +
           "AND (:productType IS NULL OR p.productType = :productType) " +
           "ORDER BY p.createdAt DESC")
    List<ProviderProduct> findTopActive(@Param("productType") ProductType productType,
                                        org.springframework.data.domain.Pageable pageable);
}
