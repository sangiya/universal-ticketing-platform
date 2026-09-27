package com.ticketmesh.service;

import com.ticketmesh.dto.AdminStatsResponse;
import com.ticketmesh.dto.OrderSummary;
import com.ticketmesh.dto.ProductTypeStat;
import com.ticketmesh.dto.ShopResponse;
import com.ticketmesh.dto.UserSummary;
import com.ticketmesh.model.AgentShop;
import com.ticketmesh.model.ProductOrder;
import com.ticketmesh.model.Provider;
import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.AgentShopRepository;
import com.ticketmesh.repository.BookingRepository;
import com.ticketmesh.repository.ProductOrderRepository;
import com.ticketmesh.repository.ProviderProductRepository;
import com.ticketmesh.repository.ProviderRepository;
import com.ticketmesh.repository.TenantRepository;
import com.ticketmesh.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Read-only operational surface for the platform admin console.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final ProviderRepository providerRepository;
    private final AgentShopRepository shopRepository;
    private final ProviderProductRepository productRepository;
    private final BookingRepository bookingRepository;
    private final ProductOrderRepository orderRepository;

    public AdminService(UserRepository userRepository,
                        TenantRepository tenantRepository,
                        ProviderRepository providerRepository,
                        AgentShopRepository shopRepository,
                        ProviderProductRepository productRepository,
                        BookingRepository bookingRepository,
                        ProductOrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.providerRepository = providerRepository;
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
        this.bookingRepository = bookingRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse dashboard() {
        List<User> users = userRepository.findAll();
        long customers = users.stream()
                .filter(u -> u.getRole() == User.Role.CUSTOMER).count();
        long agents = users.stream()
                .filter(u -> u.getRole() == User.Role.AGENT).count();
        long admins = users.stream()
                .filter(u -> u.getRole() == User.Role.ADMIN).count();

        List<Provider> providers = providerRepository.findAll();
        long activeTenants = tenantRepository.findAll().stream()
                .filter(t -> t.isEnabled()).count();
        long activeProviders = providers.stream()
                .filter(p -> p.getStatus() == Provider.Status.ACTIVE).count();
        long pendingShops = shopRepository.findByStatus(AgentShop.Status.PENDING).size();
        long approvedShops = shopRepository.findByStatus(AgentShop.Status.APPROVED).size();

        List<ProviderProduct> products = productRepository.findAll();
        long enabledProducts = products.stream().filter(ProviderProduct::isEnabled).count();

        // ---- Universal commerce metrics (every product domain, not just travel) ----
        List<ProductOrder> orders = orderRepository.findAll();
        List<ProductOrder> paidOrders = orders.stream()
                .filter(o -> o.getStatus() == ProductOrder.Status.PAID
                        || o.getStatus() == ProductOrder.Status.ISSUED
                        || o.getStatus() == ProductOrder.Status.USED)
                .toList();

        // Single-currency GMV only (never sum mixed currencies silently).
        String gmvCurrency = paidOrders.stream()
                .map(ProductOrder::getCurrencyIso)
                .filter(java.util.Objects::nonNull)
                .findFirst().orElse("LKR");
        java.math.BigDecimal gmv = paidOrders.stream()
                .filter(o -> gmvCurrency.equals(o.getCurrencyIso()))
                .map(ProductOrder::getTotalAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // Per product type (universal) with provider vertical + catalog count.
        Map<String, Long> ordersByType = paidOrders.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ProductOrder::getProductType, java.util.stream.Collectors.counting()));
        Map<String, java.math.BigDecimal> revenueByType = paidOrders.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ProductOrder::getProductType,
                        java.util.stream.Collectors.reducing(java.math.BigDecimal.ZERO,
                                ProductOrder::getTotalAmount, java.math.BigDecimal::add)));
        Map<String, Long> catalogByType = products.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.getProductType().name(), java.util.stream.Collectors.counting()));
        Map<Long, String> verticalByProvider = providers.stream()
                .collect(java.util.stream.Collectors.toMap(
                        Provider::getId, p -> p.getVertical().name(), (a, b) -> a));

        java.util.TreeSet<String> allTypes = new java.util.TreeSet<>(catalogByType.keySet());
        allTypes.addAll(ordersByType.keySet());

        List<ProductTypeStat> typeStats = allTypes.stream().map(type -> {
            long catalog = catalogByType.getOrDefault(type, 0L);
            String vertical = products.stream()
                    .filter(p -> p.getProductType().name().equals(type))
                    .map(p -> verticalByProvider.getOrDefault(p.getProvider().getId(), "OTHER"))
                    .findFirst().orElse("OTHER");
            return new ProductTypeStat(type, vertical, catalog,
                    ordersByType.getOrDefault(type, 0L),
                    revenueByType.getOrDefault(type, java.math.BigDecimal.ZERO));
        }).toList();

        return new AdminStatsResponse(
                users.size(), customers, agents, admins,
                tenantRepository.count(), activeTenants,
                providers.size(), activeProviders,
                pendingShops, approvedShops,
                products.size(), enabledProducts,
                countBookings(),
                paidOrders.size(), gmv, gmvCurrency,
                allTypes.size(), typeStats);
    }

    @Transactional(readOnly = true)
    public long countBookings() {
        return bookingRepository.count();
    }

    @Transactional(readOnly = true)
    public List<UserSummary> listUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserSummary(
                        u.getId(), u.getUsername(), u.getFullName(), u.getEmail(),
                        u.getRole().name(), u.getStatus().name(),
                        u.getTenantId(), u.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderSummary> listOrders() {
        return orderRepository.findAll().stream()
                .map(o -> new OrderSummary(
                        o.getId(), o.getOrderRef(), o.getProviderName(),
                        o.getProductTitle(), o.getProductType(), o.getQuantity(),
                        o.getUnitPrice(), o.getCurrencyIso(), o.getTotalAmount(),
                        o.getStatus().name(), o.getCreatedAt()))
                .toList();
    }
}
