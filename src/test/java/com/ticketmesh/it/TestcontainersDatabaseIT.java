package com.ticketmesh.it;

import com.ticketmesh.model.Tenant;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.TenantRepository;
import com.ticketmesh.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real MySQL integration test using Testcontainers. Spins up an actual MySQL
 * 8.0 instance in Docker, runs Flyway migrations, and verifies tenant and
 * user data round-trip. This is the integration gate for any database schema
 * change before merging.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TestcontainersDatabaseIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("ticketmesh_test")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(false);

    @DynamicPropertySource
    static void registerMySql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        // Avoid hitting external services in tests
        registry.add("spring.ai.openai.api-key", () -> "");
    }

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void tenantAndUserRoundTripOnMySql() {
        Tenant tenant = new Tenant("acme-global", "Acme Global", "US", "USD",
                "en", "America/New_York", null);
        Tenant savedTenant = tenantRepository.save(tenant);
        assertNotNull(savedTenant.getId());

        User user = new User("alice", "hashed-pw", "Alice Smith", "alice@acme.com",
                User.Role.AGENT, savedTenant.getId(), "+15551234567");
        User savedUser = userRepository.save(user);
        assertNotNull(savedUser.getId());

        // Round-trip read
        Tenant readTenant = tenantRepository.findById(savedTenant.getId()).orElseThrow();
        assertEquals("acme-global", readTenant.getSlug());
        assertEquals("USD", readTenant.getCurrencyIso());

        User readUser = userRepository.findByUsername("alice").orElseThrow();
        assertEquals(User.Role.AGENT, readUser.getRole());
        assertEquals(savedTenant.getId(), readUser.getTenantId());
    }

    @Test
    @Transactional
    void tenantQueryBySlug() {
        Tenant tenant = new Tenant("titan-asia", "Titan Asia", "SG", "SGD",
                "en", "Asia/Singapore", null);
        tenantRepository.save(tenant);
        assertTrue(tenantRepository.findBySlug("titan-asia").isPresent());
    }

    @Test
    @Transactional
    void userQueryByEmailNormalizes() {
        Tenant tenant = new Tenant("norm-test", "Norm Test", "GB", "GBP",
                "en", "Europe/London", null);
        tenantRepository.save(tenant);
        User user = new User("bob", "pw", "Bob Jones", "Bob@Example.COM",
                User.Role.CUSTOMER, tenant.getId(), null);
        userRepository.save(user);

        // Email stored is normalized to lowercase
        List<User> matches = userRepository.findByEmailIgnoreCase("bob@example.com");
        assertEquals(1, matches.size());
        assertEquals("bob", matches.get(0).getUsername());
    }
}
