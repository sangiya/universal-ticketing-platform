package com.ticketmesh.service;

import com.ticketmesh.dto.ProviderConnectRequest;
import com.ticketmesh.dto.ProviderResponse;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.AgentShop;
import com.ticketmesh.model.Provider;
import com.ticketmesh.model.Provider.ProviderVertical;
import com.ticketmesh.model.Provider.Status;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.AgentShopRepository;
import com.ticketmesh.repository.ProviderRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Model that lets an approved agent shop connect one or more ticket providers
 * (services) to the platform. Providers carry the shop's country/currency and
 * declare the capabilities they support.
 */
@Service
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final AgentShopRepository shopRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public ProviderService(ProviderRepository providerRepository,
                           AgentShopRepository shopRepository,
                           UserRepository userRepository,
                           CurrentUser currentUser) {
        this.providerRepository = providerRepository;
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public ProviderResponse connect(ProviderConnectRequest request) {
        if (providerRepository.findByCode(request.getCode()).isPresent()) {
            throw new ConflictException("Provider code already exists: " + request.getCode());
        }
        AgentShop shop = requireMyApprovedShop();
        ProviderVertical vertical = parseVertical(request.getVertical());
        Provider provider = new Provider(
                request.getCode(), request.getName(), shop, shop.getTenant(),
                shop.getCountryIso(), shop.getCurrencyIso(), shop.getTenant().getTimezone(),
                request.getApiEndpoint(), request.getAuthMode(), vertical,
                request.getCapabilities());
        providerRepository.save(provider);
        return toResponse(provider);
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> mine() {
        AgentShop shop = requireMyShop();
        return providerRepository.findByShop_Id(shop.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> listAll(Status status) {
        return providerRepository.findByStatus(status)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProviderResponse setStatus(Long providerId, Status status) {
        Provider provider = requireProvider(providerId);
        if (status != Status.ACTIVE && status != Status.SUSPENDED) {
            throw new ConflictException("Only ACTIVE or SUSPENDED actions are allowed");
        }
        provider.setStatus(status);
        providerRepository.save(provider);
        return toResponse(provider);
    }

    private Provider requireProvider(Long providerId) {
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new NotFoundException("Provider not found: " + providerId));
    }

    private AgentShop requireMyApprovedShop() {
        AgentShop shop = requireMyShop();
        if (shop.getStatus() != AgentShop.Status.APPROVED) {
            throw new ConflictException(
                    "Provider connection requires an approved shop (current: "
                            + shop.getStatus().name() + ")");
        }
        return shop;
    }

    private AgentShop requireMyShop() {
        Long userId = currentUser().getId();
        return shopRepository.findByOwner_Id(userId)
                .orElseThrow(() -> new ConflictException(
                        "You must apply for a shop before connecting providers"));
    }

    private User currentUser() {
        return userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }

    private ProviderVertical parseVertical(String raw) {
        try {
            return ProviderVertical.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("Unsupported vertical: " + raw);
        }
    }

    private ProviderResponse toResponse(Provider p) {
        return new ProviderResponse(
                p.getId(), p.getCode(), p.getName(),
                p.getShop().getId(), p.getTenant().getId(),
                p.getCountryIso(), p.getCurrencyIso(), p.getTimezone(),
                p.getApiEndpoint(), p.getAuthMode(), p.getVertical().name(),
                p.getCapabilities(), p.getStatus().name(), p.getCreatedAt());
    }
}
