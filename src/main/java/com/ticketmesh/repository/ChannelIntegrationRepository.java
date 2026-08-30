package com.ticketmesh.repository;

import com.ticketmesh.model.ChannelIntegration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelIntegrationRepository extends JpaRepository<ChannelIntegration, Long> {

    Optional<ChannelIntegration> findByTenant_IdAndChannel(Long tenantId,
                                                           ChannelIntegration.Channel channel);

    List<ChannelIntegration> findByTenant_Id(Long tenantId);
}
