package com.ticketmesh.security;

import com.ticketmesh.model.User;
import com.ticketmesh.observability.MdcLoggingFilter;
import com.ticketmesh.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserDetailsService userDetailsService,
                                   UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        String username;
        try {
            username = jwtService.extractUsername(token);
        } catch (Exception ex) {
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtService.isTokenValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Populate MDC with the authenticated user's id and tenant for
                // structured logging (spec §55).
                userRepository.findByUsername(username).ifPresent(user -> {
                    MdcLoggingFilter.setAuthenticated(
                            String.valueOf(user.getId()),
                            user.getTenantId() == null ? null : String.valueOf(user.getTenantId()));
                });
            }
        }
        filterChain.doFilter(request, response);
    }
}
