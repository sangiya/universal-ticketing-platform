package com.ticketmesh.service;

import com.ticketmesh.dto.ShopApplyRequest;
import com.ticketmesh.dto.ShopResponse;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.AgentShop;
import com.ticketmesh.model.Tenant;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.AgentShopRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lets any shop owner connect their business to the platform (Uber/PickMe
 * model). The agent applies with a shop profile; the platform admin reviews
 * and approves before the shop can list and sell products.
 */
@Service
public class AgentOnboardingService {

    private final AgentShopRepository shopRepository;
    private final UserRepository userRepository;
    private final TenantService tenantService;
    private final CurrentUser currentUser;

    public AgentOnboardingService(AgentShopRepository shopRepository,
                                  UserRepository userRepository,
                                  TenantService tenantService,
                                  CurrentUser currentUser) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.tenantService = tenantService;
        this.currentUser = currentUser;
    }

    @Transactional
    public ShopResponse apply(String tenantSlug, ShopApplyRequest request) {
        User owner = requireCurrentUser();
        if (shopRepository.findByOwner_Id(owner.getId()).isPresent()) {
            throw new ConflictException("You already have a shop application");
        }
        Tenant tenant = tenantService.requireTenant(tenantSlug);
        AgentShop shop = new AgentShop(
                owner, tenant, request.getShopName(), request.getBusinessType(),
                request.getCountryIso(), request.getCurrencyIso(), request.getAbout(),
                request.getContactEmail(), request.getContactPhone());
        shopRepository.save(shop);
        return toResponse(shop);
    }

    @Transactional(readOnly = true)
    public ShopResponse myShop() {
        AgentShop shop = requireMyShop();
        return toResponse(shop);
    }

    @Transactional(readOnly = true)
    public List<ShopResponse> listByStatus(AgentShop.Status status) {
        return shopRepository.findByStatus(status).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ShopResponse approve(Long shopId, AgentShop.Status action) {
        AgentShop shop = requireShop(shopId);
        if (action == AgentShop.Status.APPROVED) {
            shop.approve(currentUser().getId());
        } else if (action == AgentShop.Status.SUSPENDED) {
            shop.suspend(currentUser().getId());
        } else {
            throw new ConflictException("Only APPROVED or SUSPENDED actions are allowed");
        }
        shopRepository.save(shop);
        return toResponse(shop);
    }

    private AgentShop requireMyShop() {
        User owner = requireCurrentUser();
        return shopRepository.findByOwner_Id(owner.getId())
                .orElseThrow(() -> new NotFoundException("No shop found for current user"));
    }

    private AgentShop requireShop(Long shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found: " + shopId));
    }

    private User requireCurrentUser() {
        return userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }

    private User currentUser() {
        return userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }

    private ShopResponse toResponse(AgentShop s) {
        return new ShopResponse(
                s.getId(), s.getShopName(), s.getBusinessType(),
                s.getCountryIso(), s.getCurrencyIso(), s.getAbout(),
                s.getContactEmail(), s.getContactPhone(),
                s.getStatus().name(),
                s.getTenant().getId(),
                s.getAppliedAt(), s.getReviewedAt());
    }
}
