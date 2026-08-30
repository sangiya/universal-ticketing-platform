package com.ticketmesh.repository;

import com.ticketmesh.model.AgentShop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentShopRepository extends JpaRepository<AgentShop, Long> {

    Optional<AgentShop> findByOwner_Id(Long ownerUserId);

    List<AgentShop> findByStatus(AgentShop.Status status);
}
