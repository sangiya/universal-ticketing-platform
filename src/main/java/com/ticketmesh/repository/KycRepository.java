package com.ticketmesh.repository;

import com.ticketmesh.model.KycSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KycRepository extends JpaRepository<KycSubmission, Long> {
    List<KycSubmission> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<KycSubmission> findByStatusOrderByCreatedAtDesc(KycSubmission.Status status);
}
