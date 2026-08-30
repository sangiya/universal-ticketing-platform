package com.ticketmesh.service;

import com.ticketmesh.dto.BrandingRequest;
import com.ticketmesh.dto.BrandingResponse;
import com.ticketmesh.model.Tenant;
import com.ticketmesh.model.TenantBranding;
import com.ticketmesh.repository.TenantBrandingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the white-label theme/branding of a tenant. This is the
 * "change everything through configuration" surface (colors, images, logo,
 * fonts, hero copy) that makes the product installable anywhere, like a theme
 * engine on WordPress.
 */
@Service
public class BrandingService {

    private static final String DEFAULT_PRIMARY = "#4F46E5";

    private final TenantBrandingRepository brandingRepository;
    private final TenantService tenantService;

    public BrandingService(TenantBrandingRepository brandingRepository,
                           TenantService tenantService) {
        this.brandingRepository = brandingRepository;
        this.tenantService = tenantService;
    }

    @Transactional
    public BrandingResponse upsert(String slug, BrandingRequest request) {
        Tenant tenant = tenantService.requireTenant(slug);
        TenantBranding branding = brandingRepository.findByTenant(tenant)
                .orElseGet(() -> new TenantBranding(tenant,
                        request.getBrandName(), request.getPrimaryColor()));
        applyRequest(branding, request);
        branding.touch();
        brandingRepository.save(branding);
        return toResponse(branding);
    }

    @Transactional(readOnly = true)
    public BrandingResponse getPublic(String slug) {
        Tenant tenant = tenantService.requireTenant(slug);
        TenantBranding branding = brandingRepository.findByTenant(tenant)
                .orElseGet(() -> new TenantBranding(tenant,
                        tenant.getName(), DEFAULT_PRIMARY));
        return toResponse(branding);
    }

    private void applyRequest(TenantBranding b, BrandingRequest r) {
        if (r.getBrandName() != null) {
            b.setBrandName(r.getBrandName());
        }
        b.setTagline(r.getTagline());
        if (r.getPrimaryColor() != null) {
            b.setPrimaryColor(r.getPrimaryColor());
        }
        b.setSecondaryColor(r.getSecondaryColor());
        b.setAccentColor(r.getAccentColor());
        b.setLogoUrl(r.getLogoUrl());
        b.setBannerUrl(r.getBannerUrl());
        b.setAppIconUrl(r.getAppIconUrl());
        b.setFontFamily(r.getFontFamily());
        if (r.getBorderRadius() != null) {
            b.setBorderRadius(r.getBorderRadius());
        }
        if (r.getDarkMode() != null) {
            b.setDarkMode(r.getDarkMode());
        }
        b.setHomeHeroTitle(r.getHomeHeroTitle());
        b.setHomeHeroSubtitle(r.getHomeHeroSubtitle());
    }

    private BrandingResponse toResponse(TenantBranding b) {
        return new BrandingResponse(
                b.getTenant().getId(),
                b.getTenant().getSlug(),
                b.getBrandName(),
                b.getTagline(),
                b.getPrimaryColor(),
                b.getSecondaryColor(),
                b.getAccentColor(),
                b.getLogoUrl(),
                b.getBannerUrl(),
                b.getAppIconUrl(),
                b.getFontFamily(),
                b.getBorderRadius(),
                b.isDarkMode(),
                b.getHomeHeroTitle(),
                b.getHomeHeroSubtitle(),
                b.getUpdatedAt());
    }
}
