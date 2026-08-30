package com.ticketmesh.service;

import com.ticketmesh.dto.AuthResponse;
import com.ticketmesh.dto.LoginRequest;
import com.ticketmesh.dto.RegisterRequest;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.Tenant;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.TenantRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }
        User.Role role = resolveRole(request.getRole());
        Long tenantId = resolveTenant(request.getTenantSlug());
        if (role == User.Role.AGENT && tenantId == null) {
            tenantId = provisionAgentTenant(request);
        }
        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getEmail(),
                role,
                tenantId,
                request.getPhone());
        userRepository.save(user);
        return authenticate(request.getUsername(), request.getPassword());
    }

    private User.Role resolveRole(String raw) {
        if (raw == null || raw.isBlank()) {
            return User.Role.CUSTOMER;
        }
        User.Role role;
        try {
            role = User.Role.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("Unsupported role: " + raw);
        }
        if (role == User.Role.ADMIN) {
            throw new ConflictException("Admin accounts cannot be self-registered");
        }
        return role;
    }

    private Long resolveTenant(String tenantSlug) {
        if (tenantSlug == null || tenantSlug.isBlank()) {
            return null;
        }
        Tenant tenant = tenantRepository.findBySlug(tenantSlug.toLowerCase())
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantSlug));
        return tenant.getId();
    }

    /**
     * Uber/PickMe style activation: an agent with no tenant gets its own shop
     * tenant, ready to sell immediately (moderation mode INSTANT).
     */
    private Long provisionAgentTenant(RegisterRequest request) {
        String slug = uniqueAgentSlug(request.getUsername());
        Tenant tenant = new Tenant(
                slug,
                request.getFullName() + " Shop",
                defaultIfBlank(request.getCountryIso(), "LK"),
                defaultIfBlank(request.getCurrencyIso(), "LKR"),
                defaultIfBlank(request.getDefaultLanguage(), "en"),
                defaultIfBlank(request.getTimezone(), "Asia/Colombo"),
                null);
        Tenant saved = tenantRepository.save(tenant);
        return saved.getId();
    }

    private String uniqueAgentSlug(String username) {
        String base = username == null ? "agent" : username;
        String slug = base.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            slug = "agent";
        }
        slug = slug + "-shop";
        String candidate = slug;
        int n = 2;
        while (tenantRepository.existsBySlug(candidate)) {
            candidate = slug + "-" + n;
            n++;
        }
        return candidate;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public AuthResponse login(LoginRequest request) {
        return authenticate(request.getUsername(), request.getPassword());
    }

    private AuthResponse authenticate(String username, String rawPassword) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, rawPassword));
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String token = jwtService.generateToken(userDetails);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ConflictException("User not found: " + username));
        String role = user.getRole().name();
        return new AuthResponse(token, user.getUsername(), user.getFullName(), role);
    }
}
