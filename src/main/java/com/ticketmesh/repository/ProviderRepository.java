package com.ticketmesh.repository;

import com.ticketmesh.model.Provider;
import com.ticketmesh.model.Provider.ProviderVertical;
import com.ticketmesh.model.Provider.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    Optional<Provider> findByCode(String code);

    List<Provider> findByStatus(Status status);

    List<Provider> findByVertical(ProviderVertical vertical);

    List<Provider> findByShop_Id(Long shopId);
}
