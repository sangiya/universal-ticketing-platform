package com.ticketmesh.security;

import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user");
        }
        return auth.getName();
    }

    public Long userId() {
        User user = userRepository.findByUsername(username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        return user.getId();
    }

    public User user() {
        return userRepository.findByUsername(username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }

    public UserDetails userDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user");
        }
        if (auth.getPrincipal() instanceof UserDetails ud) {
            return ud;
        }
        throw new IllegalStateException("Principal is not a UserDetails");
    }
}
