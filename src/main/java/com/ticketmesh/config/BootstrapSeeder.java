package com.ticketmesh.config;

import com.ticketmesh.model.Tenant;
import com.ticketmesh.model.TenantBranding;
import com.ticketmesh.model.User;
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

/**
 * Idempotent bootstrap that ensures a default global tenant with branding and a
 * platform-admin account exist so the product is usable straight out of the
 * box, locally and in any deployment environment.
 */
@Component
public class BootstrapSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapSeeder.class);

    private final TenantRepository tenantRepository;
    private final TenantBrandingRepository brandingRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public BootstrapSeeder(TenantRepository tenantRepository,
                           TenantBrandingRepository brandingRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.bootstrap.admin-username:admin}") String adminUsername,
                           @Value("${app.bootstrap.admin-password:ChangeMe123!}") String adminPassword) {
        this.tenantRepository = tenantRepository;
        this.brandingRepository = brandingRepository;
        this.userRepository = userRepository;
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

        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = new User(
                    adminUsername,
                    passwordEncoder.encode(adminPassword),
                    "Platform Administrator",
                    "admin@ticketmesh.local",
                    User.Role.ADMIN,
                    tenant.getId());
            userRepository.save(admin);
            log.info("Bootstrapped default admin account: {}", adminUsername);
        }
    }
}
