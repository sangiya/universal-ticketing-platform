package com.ticketmesh.service;

import com.ticketmesh.dto.TenantRequest;
import com.ticketmesh.dto.TenantResponse;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.Tenant;
import com.ticketmesh.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public TenantResponse create(TenantRequest request) {
        if (tenantRepository.existsBySlug(request.getSlug().toLowerCase())) {
            throw new ConflictException("Tenant slug already exists: " + request.getSlug());
        }
        Tenant tenant = new Tenant(
                request.getSlug().toLowerCase(),
                request.getName(),
                request.getCountryIso(),
                request.getCurrencyIso(),
                request.getDefaultLanguage(),
                request.getTimezone(),
                request.getDomain());
        tenantRepository.save(tenant);
        return toResponse(tenant);
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> list() {
        return tenantRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TenantResponse getBySlug(String slug) {
        Tenant tenant = requireTenant(slug);
        return toResponse(tenant);
    }

    @Transactional
    public TenantResponse update(String slug, TenantRequest request) {
        Tenant tenant = requireTenant(slug);
        tenant.setName(request.getName());
        tenant.setCountryIso(request.getCountryIso());
        tenant.setCurrencyIso(request.getCurrencyIso());
        tenant.setDefaultLanguage(request.getDefaultLanguage());
        tenant.setTimezone(request.getTimezone());
        tenant.setDomain(request.getDomain());
        tenant.bumpConfigVersion();
        tenantRepository.save(tenant);
        return toResponse(tenant);
    }

    @Transactional
    public TenantResponse setEnabled(String slug, boolean enabled) {
        Tenant tenant = requireTenant(slug);
        tenant.setEnabled(enabled);
        tenantRepository.save(tenant);
        return toResponse(tenant);
    }

    @Transactional
    public TenantResponse setModerationMode(String slug, Tenant.ModerationMode mode) {
        Tenant tenant = requireTenant(slug);
        tenant.setModerationMode(mode);
        tenant.bumpConfigVersion();
        tenantRepository.save(tenant);
        return toResponse(tenant);
    }

    @Transactional(readOnly = true)
    public Tenant requireTenant(String slug) {
        return tenantRepository.findBySlug(slug.toLowerCase())
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + slug));
    }

    @Transactional(readOnly = true)
    public Tenant requireTenantById(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantId));
    }

    private TenantResponse toResponse(Tenant t) {
        return new TenantResponse(
                t.getId(), t.getSlug(), t.getName(), t.getCountryIso(), t.getCurrencyIso(),
                t.getDefaultLanguage(), t.getTimezone(), t.getDomain(), t.isEnabled(),
                t.getConfigVersion(), t.getModerationMode().name());
    }
}
