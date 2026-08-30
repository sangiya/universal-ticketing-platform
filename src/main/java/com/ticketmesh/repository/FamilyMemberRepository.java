package com.ticketmesh.repository;

import com.ticketmesh.model.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

    List<FamilyMember> findByFamilyId(Long familyId);

    List<FamilyMember> findByUserId(Long userId);

    Optional<FamilyMember> findByFamilyIdAndUserId(Long familyId, Long userId);

    boolean existsByFamilyIdAndUserId(Long familyId, Long userId);

    long countByFamilyId(Long familyId);
}
