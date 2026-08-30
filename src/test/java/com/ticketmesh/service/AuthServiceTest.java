package com.ticketmesh.service;

import com.ticketmesh.dto.AuthResponse;
import com.ticketmesh.dto.RegisterRequest;
import com.ticketmesh.model.Tenant;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.TenantRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private TenantRepository tenantRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private UserDetailsService userDetailsService;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tenantRepository = mock(TenantRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authenticationManager = mock(AuthenticationManager.class);
        userDetailsService = mock(UserDetailsService.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(userRepository, tenantRepository, passwordEncoder,
                authenticationManager, userDetailsService, jwtService);

        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenReturn(new org.springframework.security.core.userdetails.User(
                        "user", "pw", List.of()));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private User agentUser() {
        return new User("agentShop", "encoded", "Agent Shop", "agent@shop.com",
                User.Role.AGENT, 42L, "0771234567");
    }

    private RegisterRequest agentRequestWithoutTenant() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("agentShop");
        req.setPassword("strongpass123");
        req.setFullName("Agent Shop");
        req.setEmail("agent@shop.com");
        req.setRole("AGENT");
        req.setPhone("0771234567");
        return req;
    }

    @Test
    void register_agentWithoutTenantAutoProvisionsTenant() {
        when(userRepository.findByUsername("agentShop")).thenReturn(Optional.of(agentUser()));

        AuthResponse response = authService.register(agentRequestWithoutTenant());

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        Tenant provisioned = tenantCaptor.getValue();
        assertEquals("agentshop-shop", provisioned.getSlug());
        assertEquals("Agent Shop Shop", provisioned.getName());
        assertEquals("LK", provisioned.getCountryIso());
        assertEquals("LKR", provisioned.getCurrencyIso());
        assertEquals("en", provisioned.getDefaultLanguage());
        assertEquals("Asia/Colombo", provisioned.getTimezone());
        assertEquals(Tenant.ModerationMode.INSTANT, provisioned.getModerationMode());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals(provisioned.getId(), saved.getTenantId());
        assertEquals("0771234567", saved.getPhone());
        assertEquals(User.Role.AGENT, saved.getRole());

        assertEquals("jwt-token", response.token());
        assertEquals("agentShop", response.username());
        assertEquals("AGENT", response.role());
    }

    @Test
    void register_agentWithCollidingSlugGetsUniqueSuffix() {
        when(userRepository.findByUsername("agentShop")).thenReturn(Optional.of(agentUser()));
        when(tenantRepository.existsBySlug("agentshop-shop")).thenReturn(true);
        when(tenantRepository.existsBySlug("agentshop-shop-2")).thenReturn(false);

        authService.register(agentRequestWithoutTenant());

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(captor.capture());
        assertEquals("agentshop-shop-2", captor.getValue().getSlug());
    }

    @Test
    void register_agentWithExistingTenantDoesNotProvision() {
        Tenant tenant = new Tenant("demo-sg", "Demo Singapore", "SG", "SGD",
                "en", "Asia/Singapore", null);
        ReflectionTestUtils.setField(tenant, "id", 7L);
        when(tenantRepository.findBySlug("demo-sg")).thenReturn(Optional.of(tenant));
        when(userRepository.findByUsername("shopOwner")).thenReturn(Optional.of(
                new User("shopOwner", "encoded", "Shop Owner", "owner@example.com",
                        User.Role.AGENT, tenant.getId(), "0770000000")));

        RegisterRequest req = new RegisterRequest();
        req.setUsername("shopOwner");
        req.setPassword("strongpass123");
        req.setFullName("Shop Owner");
        req.setEmail("owner@example.com");
        req.setRole("AGENT");
        req.setTenantSlug("demo-sg");

        authService.register(req);

        verify(tenantRepository, never()).save(any(Tenant.class));
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(tenant.getId(), userCaptor.getValue().getTenantId());
    }

    @Test
    void register_customerDoesNotProvisionTenant() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(
                new User("bob", "encoded", "Bob", "bob@example.com",
                        User.Role.CUSTOMER, null, null)));

        RegisterRequest req = new RegisterRequest();
        req.setUsername("bob");
        req.setPassword("strongpass123");
        req.setFullName("Bob");
        req.setEmail("bob@example.com");

        authService.register(req);

        verify(tenantRepository, never()).save(any(Tenant.class));
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertNull(userCaptor.getValue().getTenantId());
    }
}