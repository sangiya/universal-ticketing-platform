package com.ticketmesh.repository;

import com.ticketmesh.model.UserMfa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserMfaRepository extends JpaRepository<UserMfa, Long> {
    Optional<UserMfa> findByUserId(Long userId);
}
