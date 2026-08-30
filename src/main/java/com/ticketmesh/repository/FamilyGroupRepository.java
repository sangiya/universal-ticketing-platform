package com.ticketmesh.repository;

import com.ticketmesh.model.FamilyGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamilyGroupRepository extends JpaRepository<FamilyGroup, Long> {

    List<FamilyGroup> findByTenantId(Long tenantId);
}
