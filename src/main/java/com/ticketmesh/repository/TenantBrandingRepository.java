package com.ticketmesh.repository;

import com.ticketmesh.model.Tenant;
import com.ticketmesh.model.TenantBranding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantBrandingRepository extends JpaRepository<TenantBranding, Long> {

    Optional<TenantBranding> findByTenant(Tenant tenant);

    Optional<TenantBranding> findByTenant_Slug(String slug);
}
