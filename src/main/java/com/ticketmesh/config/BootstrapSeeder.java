package com.ticketmesh.config;

import com.ticketmesh.model.AgentShop;
import com.ticketmesh.model.Provider;
import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.model.Tenant;
import com.ticketmesh.model.TenantBranding;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.AgentShopRepository;
import com.ticketmesh.repository.ProviderProductRepository;
import com.ticketmesh.repository.ProviderRepository;
import com.ticketmesh.repository.TenantBrandingRepository;
import com.ticketmesh.repository.TenantRepository;
import com.ticketmesh.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Idempotent bootstrap that ensures a default global tenant with branding, a
 * platform-admin account and a UNIVERSAL multi-domain demo catalog (bus, train,
 * movie, event, sports, flight, ferry, attraction) exist so the product is
 * usable straight out of the box, locally and in any deployment environment.
 */
@Component
public class BootstrapSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapSeeder.class);

    private final TenantRepository tenantRepository;
    private final TenantBrandingRepository brandingRepository;
    private final UserRepository userRepository;
    private final AgentShopRepository shopRepository;
    private final ProviderRepository providerRepository;
    private final ProviderProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public BootstrapSeeder(TenantRepository tenantRepository,
                           TenantBrandingRepository brandingRepository,
                           UserRepository userRepository,
                           AgentShopRepository shopRepository,
                           ProviderRepository providerRepository,
                           ProviderProductRepository productRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.bootstrap.admin-username:admin}") String adminUsername,
                           @Value("${app.bootstrap.admin-password:ChangeMe123!}") String adminPassword) {
        this.tenantRepository = tenantRepository;
        this.brandingRepository = brandingRepository;
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.providerRepository = providerRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Tenant tenant = tenantRepository.findBySlug("global")
                .orElseGet(() -> {
                    Tenant t = new Tenant(
                            "global", "Global Ticketing", "LK", "LKR", "en",
                            "Asia/Colombo", null);
                    tenantRepository.save(t);
                    log.info("Bootstrapped default global tenant");
                    return t;
                });

        if (brandingRepository.findByTenant(tenant).isEmpty()) {
            TenantBranding branding = new TenantBranding(tenant, "TicketMesh", "#4F46E5");
            branding.setTagline("All your tickets, one platform");
            brandingRepository.save(branding);
            log.info("Bootstrapped default global branding");
        }

        User admin = userRepository.findByUsername(adminUsername).orElseGet(() -> {
            User u = new User(
                    adminUsername,
                    passwordEncoder.encode(adminPassword),
                    "Platform Administrator",
                    "admin@ticketmesh.local",
                    User.Role.ADMIN,
                    tenant.getId());
            userRepository.save(u);
            log.info("Bootstrapped default admin account: {}", adminUsername);
            return u;
        });

        User customer = userRepository.findByUsername("customer").orElseGet(() -> {
            User u = new User(
                    "customer", passwordEncoder.encode("Customer123!"),
                    "Casey Customer", "customer@ticketmesh.local",
                    User.Role.CUSTOMER, tenant.getId());
            userRepository.save(u);
            log.info("Bootstrapped demo customer account");
            return u;
        });

        User agent = userRepository.findByUsername("agent").orElseGet(() -> {
            User u = new User(
                    "agent", passwordEncoder.encode("AgentPass123!"),
                    "Avery Agent", "agent@ticketmesh.local",
                    User.Role.AGENT, tenant.getId());
            userRepository.save(u);
            log.info("Bootstrapped demo agent account");
            return u;
        });

        if (providerRepository.count() == 0) {
            seedUniversalCatalog(tenant, agent, customer);
        }
    }

    /**
     * Seeds one approved shop owning providers across every supported vertical so
     * the marketplace demonstrates the universal (non-travel) product model.
     */
    private void seedUniversalCatalog(Tenant tenant, User agent, User customer) {
        AgentShop shop = shopRepository.findByOwner_Id(agent.getId()).orElseGet(() -> {
            AgentShop s = new AgentShop(agent, tenant, "TicketMesh Showcase",
                    "Multi-Domain Retailer", "LK", "LKR",
                    "Universal marketplace demo shop selling transport, cinema, events and attractions.",
                    "shop@ticketmesh.local", "0770000000");
            s.approve(agent.getId());
            shopRepository.save(s);
            return s;
        });

        record Seed(Provider.ProviderVertical vertical, String code, String name,
                    String color, String tagline, String capabilities,
                    List<ProviderProduct.ProductType> types) {}

        List<Seed> seeds = List.of(
                new Seed(Provider.ProviderVertical.BUS, "SHOWCASE-BUS", "CityBus Lines",
                        "#0ea5e9", "Every city, every day", "search,availability,booking",
                        List.of(ProviderProduct.ProductType.ROUTE, ProviderProduct.ProductType.SEAT)),
                new Seed(Provider.ProviderVertical.TRAIN, "SHOWCASE-RAIL", "MetroRail",
                        "#4f46e5", "Fast intercity journeys", "search,availability,booking,timetable",
                        List.of(ProviderProduct.ProductType.ROUTE, ProviderProduct.ProductType.SEAT)),
                new Seed(Provider.ProviderVertical.MOVIE, "SHOWCASE-CINE", "CineWorld Cinemas",
                        "#f59e0b", "Now showing near you", "search,showtimes,seatmap,booking",
                        List.of(ProviderProduct.ProductType.ADMISSION, ProviderProduct.ProductType.SEAT)),
                new Seed(Provider.ProviderVertical.EVENT, "SHOWCASE-LIVE", "LiveStage Events",
                        "#ec4899", "Concerts, festivals and live shows", "search,availability,booking",
                        List.of(ProviderProduct.ProductType.ADMISSION)),
                new Seed(Provider.ProviderVertical.SPORTS, "SHOWCASE-ARENA", "Arena Sports",
                        "#10b981", "Match day tickets", "search,availability,seatmap,booking",
                        List.of(ProviderProduct.ProductType.ADMISSION, ProviderProduct.ProductType.SEAT)),
                new Seed(Provider.ProviderVertical.FLIGHT, "SHOWCASE-AIR", "SkyLink Airlines",
                        "#6366f1", "Fly more, pay less", "search,availability,booking",
                        List.of(ProviderProduct.ProductType.ROUTE, ProviderProduct.ProductType.SERVICE)),
                new Seed(Provider.ProviderVertical.FERRY, "SHOWCASE-MARINE", "IslandFerry",
                        "#06b6d4", "Island connections", "search,availability,booking",
                        List.of(ProviderProduct.ProductType.ROUTE, ProviderProduct.ProductType.TICKET)),
                new Seed(Provider.ProviderVertical.ATTRACTION, "SHOWCASE-VISIT", "VisitTours Attractions",
                        "#8b5cf6", "Tours, museums and activities", "search,availability,booking,timetable",
                        List.of(ProviderProduct.ProductType.ADMISSION, ProviderProduct.ProductType.PACKAGE))
        );

        for (Seed seed : seeds) {
            Provider provider = new Provider(seed.code(), seed.name(), shop, tenant,
                    "LK", "LKR", "Asia/Colombo", null, "API_KEY",
                    seed.vertical(), seed.capabilities());
            provider.setStatus(Provider.Status.ACTIVE);
            provider.setThemeColor(seed.color());
            provider.setTagline(seed.tagline());
            providerRepository.save(provider);

            LocalDateTime soon = LocalDateTime.now().plusDays(3);
            int index = 0;
            for (ProviderProduct.ProductType type : seed.types()) {
                index++;
                String title = productTitle(seed.vertical(), type, index);
                ProviderProduct product = new ProviderProduct(
                        provider, tenant, type, title,
                        originFor(seed.vertical(), type), destinationFor(seed.vertical(), type),
                        soon.plusDays(index), priceFor(type, index),
                        "LKR", 40 + (index * 7),
                        descriptionFor(seed.vertical(), type), null);
                productRepository.save(product);
            }
        }
        log.info("Seeded universal demo catalog across {} verticals for shop {}",
                seeds.size(), shop.getId());
    }

    private String productTitle(Provider.ProviderVertical v, ProviderProduct.ProductType t, int i) {
        return switch (v) {
            case BUS -> switch (t) {
                case ROUTE -> "CityBus Route Pass " + i;
                case SEAT -> "CityBus Reserved Seat " + i;
                default -> "CityBus Day Ticket " + i;
            };
            case TRAIN -> switch (t) {
                case ROUTE -> "MetroRail Intercity Route " + i;
                case SEAT -> "MetroRail First Class Seat " + i;
                default -> "MetroRail Season Pass " + i;
            };
            case MOVIE -> t == ProviderProduct.ProductType.SEAT
                    ? "CineWorld Recliner Seat " + i : "CineWorld Movie Screening " + i;
            case EVENT -> "LiveStage Concert Entry " + i;
            case SPORTS -> t == ProviderProduct.ProductType.SEAT
                    ? "Arena VIP Seat " + i : "Arena Match Ticket " + i;
            case FLIGHT -> t == ProviderProduct.ProductType.SERVICE
                    ? "SkyLink Priority Upgrade " + i : "SkyLink Flight Route " + i;
            case FERRY -> t == ProviderProduct.ProductType.TICKET
                    ? "IslandFerry Single Ticket " + i : "IslandFerry Route " + i;
            case ATTRACTION -> t == ProviderProduct.ProductType.PACKAGE
                    ? "VisitTours Full Day Package " + i : "VisitTours Attraction Entry " + i;
            default -> "Showcase Service " + i;
        };
    }

    private String originFor(Provider.ProviderVertical v, ProviderProduct.ProductType t) {
        boolean routeLike = t == ProviderProduct.ProductType.ROUTE;
        return switch (v) {
            case BUS -> routeLike ? "Central Depot" : null;
            case TRAIN -> routeLike ? "Central Station" : null;
            case FLIGHT -> routeLike ? "International Airport" : null;
            case FERRY -> routeLike ? "Main Harbour" : null;
            default -> null;
        };
    }

    private String destinationFor(Provider.ProviderVertical v, ProviderProduct.ProductType t) {
        boolean routeLike = t == ProviderProduct.ProductType.ROUTE;
        return switch (v) {
            case BUS -> routeLike ? "Airport Terminal" : null;
            case TRAIN -> routeLike ? "Riverside" : null;
            case FLIGHT -> routeLike ? "City Airport" : null;
            case FERRY -> routeLike ? "Island Port" : null;
            default -> null;
        };
    }

    private BigDecimal priceFor(ProviderProduct.ProductType t, int i) {
        return switch (t) {
            case ADMISSION -> new BigDecimal(1200).add(new BigDecimal(150).multiply(BigDecimal.valueOf(i)));
            case ROUTE -> new BigDecimal(2400).add(new BigDecimal(300).multiply(BigDecimal.valueOf(i)));
            case SEAT -> new BigDecimal(600).add(new BigDecimal(90).multiply(BigDecimal.valueOf(i)));
            case SERVICE -> new BigDecimal(4500).add(new BigDecimal(500).multiply(BigDecimal.valueOf(i)));
            case PACKAGE -> new BigDecimal(9800).add(new BigDecimal(900).multiply(BigDecimal.valueOf(i)));
            default -> new BigDecimal(1000);
        };
    }

    private String descriptionFor(Provider.ProviderVertical v, ProviderProduct.ProductType t) {
        return switch (v) {
            case BUS -> "Comfortable air-conditioned city coach service with live seat availability.";
            case TRAIN -> "High-speed intercity service with reserved seating and timetable.";
            case MOVIE -> "Cinema screening with showtimes, formats and seat selection.";
            case EVENT -> "Live event entry ticket with digital QR delivery.";
            case SPORTS -> "Match day admission including seat category selection.";
            case FLIGHT -> "Scheduled passenger flight with fare options and ancillaries.";
            case FERRY -> "Scheduled passenger ferry crossing with vehicle and cabin options.";
            case ATTRACTION -> t == ProviderProduct.ProductType.PACKAGE
                    ? "Guided full-day package bundling entries and transfers."
                    : "Single attraction entry with timed slots.";
            default -> "Universal ticketMesh service offering.";
        };
    }
}
