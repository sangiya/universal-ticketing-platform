package com.ticketmesh.repository;

import com.ticketmesh.model.SavedTraveler;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SavedTravelerRepository extends JpaRepository<SavedTraveler, Long> {
    List<SavedTraveler> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);
    List<SavedTraveler> findByTenantIdAndOwnerUserId(Long tenantId, Long ownerUserId);
}
