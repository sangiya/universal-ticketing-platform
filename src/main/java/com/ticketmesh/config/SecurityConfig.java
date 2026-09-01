package com.ticketmesh.config;

import com.ticketmesh.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login",
                                "/api/auth/refresh", "/api/auth/forgot-password",
                                "/api/auth/reset-password", "/api/auth/otp/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/trains/**").permitAll()
                        .requestMatchers("/api/tickets/verify").permitAll()
                        .requestMatchers("/api/messaging/webhook/**").permitAll()
                        .requestMatchers("/api/referrals/validate").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tenant/*/branding").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/catalog/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/movies/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/search/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/offers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/promotions/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health/**").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/api/orders/**").authenticated()
                        .requestMatchers("/api/bookings/**").authenticated()
                        .requestMatchers("/api/payments/**").authenticated()
                        .requestMatchers("/api/tickets/**").authenticated()
                        .requestMatchers("/api/ai/**").authenticated()
                        .requestMatchers("/api/agent/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(eh -> eh.authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write(
                            "{\"error\":\"Unauthorized\",\"message\":"
                                    + "\"Authentication required to access this resource\"}");
                }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
