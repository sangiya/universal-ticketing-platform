package com.ticketmesh.repository;

import com.ticketmesh.model.I18nMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface I18nMessageRepository extends JpaRepository<I18nMessage, Long> {

    List<I18nMessage> findByTenantIdAndLocale(Long tenantId, String locale);

    Optional<I18nMessage> findByTenantIdAndLocaleAndMessageKey(Long tenantId, String locale,
                                                               String messageKey);

    Optional<I18nMessage> findByTenantIdIsNullAndLocaleAndMessageKey(String locale,
                                                                     String messageKey);

    List<I18nMessage> findByTenantIdIsNullAndLocale(String locale);
}
