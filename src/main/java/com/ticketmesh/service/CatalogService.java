package com.ticketmesh.service;

import com.ticketmesh.dto.CatalogSearchResult;
import com.ticketmesh.dto.ProductRequest;
import com.ticketmesh.dto.ProductResponse;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.AgentShop;
import com.ticketmesh.model.Provider;
import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.model.ProviderProduct.ProductType;
import com.ticketmesh.repository.AgentShopRepository;
import com.ticketmesh.repository.ProviderProductRepository;
import com.ticketmesh.repository.ProviderRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Inventory/catalog management. Agents upload ticket details and services
 * against their providers; customers search and browse the live catalog of the
 * enabled products of their tenant.
 */
@Service
public class CatalogService {

    private final ProviderProductRepository productRepository;
    private final ProviderRepository providerRepository;
    private final AgentShopRepository shopRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public CatalogService(ProviderProductRepository productRepository,
                          ProviderRepository providerRepository,
                          AgentShopRepository shopRepository,
                          UserRepository userRepository,
                          CurrentUser currentUser) {
        this.productRepository = productRepository;
        this.providerRepository = providerRepository;
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public ProductResponse upload(String providerCode, ProductRequest request) {
        Provider provider = requireOwnProvider(providerCode);
        ProductType type = parseType(request.getProductType());
        ProviderProduct product = new ProviderProduct(
                provider, provider.getTenant(), type, request.getTitle(),
                request.getOrigin(), request.getDestination(), request.getEventDate(),
                request.getPrice(), request.getCurrencyIso(), request.getAvailableQuantity(),
                request.getDescription(), request.getAttributes());
        productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(Long productId, ProductRequest request) {
        ProviderProduct product = requireOwnProduct(productId);
        product.setTitle(request.getTitle());
        product.setOrigin(request.getOrigin());
        product.setDestination(request.getDestination());
        product.setEventDate(request.getEventDate());
        product.setPrice(request.getPrice());
        product.setCurrencyIso(request.getCurrencyIso());
        product.setAvailableQuantity(request.getAvailableQuantity());
        product.setDescription(request.getDescription());
        product.setAttributes(request.getAttributes());
        product.touch();
        productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse setEnabled(Long productId, boolean enabled) {
        ProviderProduct product = requireOwnProduct(productId);
        product.setEnabled(enabled);
        product.touch();
        productRepository.save(product);
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> myProviderProducts() {
        Provider provider = requireOwnProviderRef();
        return productRepository.findByProvider_Id(provider.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchCustomer(Long tenantId, String productType) {
        if (productType != null && !productType.isBlank()) {
            ProductType type = parseType(productType);
            return productRepository.findByProductTypeAndTenant_IdAndEnabledTrue(type, tenantId)
                    .stream().map(this::toResponse).toList();
        }
        return productRepository.findByTenant_IdAndEnabledTrue(tenantId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long productId) {
        return toResponse(productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException(
                        "Product not found: " + productId)));
    }

    @Transactional(readOnly = true)
    public List<CatalogSearchResult> searchText(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return productRepository.searchEnabledByText(query)
                .stream()
                .map(p -> new CatalogSearchResult(
                        p.getId(),
                        p.getTitle(),
                        p.getProvider().getName(),
                        p.getPrice(),
                        p.getCurrencyIso()))
                .toList();
    }

    private Provider requireOwnProvider(String providerCode) {
        Provider provider = requireOwnProviderRef();
        if (!provider.getCode().equals(providerCode)) {
            throw new NotFoundException("Provider not found: " + providerCode);
        }
        return provider;
    }

    private Provider requireOwnProviderRef() {
        AgentShop shop = requireMyShop();
        List<Provider> providers = providerRepository.findByShop_Id(shop.getId());
        if (providers.isEmpty()) {
            throw new ConflictException(
                    "Connect a provider first before managing catalog products");
        }
        return providers.get(0);
    }

    private ProviderProduct requireOwnProduct(Long productId) {
        ProviderProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));
        Provider provider = providerRepository.findById(product.getProvider().getId())
                .orElseThrow(() -> new NotFoundException("Provider not found"));
        AgentShop shop = shopRepository.findById(provider.getShop().getId())
                .orElseThrow(() -> new NotFoundException("Shop not found"));
        if (!shop.getOwner().getId().equals(currentUser().getId())) {
            throw new NotFoundException("Product not found: " + productId);
        }
        return product;
    }

    private AgentShop requireMyShop() {
        Long userId = currentUser().getId();
        return shopRepository.findByOwner_Id(userId)
                .orElseThrow(() -> new ConflictException(
                        "You must apply for a shop before managing catalog products"));
    }

    private com.ticketmesh.model.User currentUser() {
        return userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }

    private ProductType parseType(String raw) {
        try {
            return ProductType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("Unsupported product type: " + raw);
        }
    }

    private ProductResponse toResponse(ProviderProduct p) {
        return new ProductResponse(
                p.getId(), p.getProvider().getId(), p.getProvider().getName(),
                p.getTenant().getId(), p.getProductType().name(), p.getTitle(),
                p.getOrigin(), p.getDestination(), p.getEventDate(), p.getPrice(),
                p.getCurrencyIso(), p.getAvailableQuantity(), p.getDescription(),
                p.getAttributes(), p.isEnabled(), p.getCreatedAt());
    }
}
