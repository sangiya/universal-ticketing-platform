package com.ticketmesh.service;

import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.ExchangeRate;
import com.ticketmesh.model.I18nMessage;
import com.ticketmesh.model.Tenant;
import com.ticketmesh.repository.ExchangeRateRepository;
import com.ticketmesh.repository.I18nMessageRepository;
import com.ticketmesh.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Globalization: multi-language (i18n) and multi-currency. Any country / any
 * currency / any language, all configurable, no code. UI strings resolve to the
 * tenant's locale with a global fallback; money converts through configurable
 * exchange rates.
 */
@Service
public class GlobalizationService {

    private final I18nMessageRepository messageRepository;
    private final ExchangeRateRepository rateRepository;
    private final TenantRepository tenantRepository;

    public GlobalizationService(I18nMessageRepository messageRepository,
                                ExchangeRateRepository rateRepository,
                                TenantRepository tenantRepository) {
        this.messageRepository = messageRepository;
        this.rateRepository = rateRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public Tenant requireTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantId));
    }

    @Transactional(readOnly = true)
    public String resolve(Long tenantId, String locale, String messageKey) {
        if (tenantId != null) {
            Optional<I18nMessage> t = messageRepository
                    .findByTenantIdAndLocaleAndMessageKey(tenantId, locale, messageKey);
            if (t.isPresent()) {
                return t.get().getMessageValue();
            }
        }
        Optional<I18nMessage> g = messageRepository
                .findByTenantIdIsNullAndLocaleAndMessageKey(locale, messageKey);
        if (g.isPresent()) {
            return g.get().getMessageValue();
        }
        return messageKey;
    }

    @Transactional
    public I18nMessage upsert(Long tenantId, String locale, String messageKey,
                              String messageValue) {
        Optional<I18nMessage> existing = messageRepository
                .findByTenantIdAndLocaleAndMessageKey(tenantId, locale, messageKey);
        if (existing.isPresent()) {
            existing.get().setMessageValue(messageValue);
            return messageRepository.save(existing.get());
        }
        return messageRepository.save(
                new I18nMessage(tenantId, locale, messageKey, messageValue));
    }

    @Transactional(readOnly = true)
    public Map<String, String> dictionary(Long tenantId, String locale) {
        Map<String, String> result = new LinkedHashMap<>();
        List<I18nMessage> global = messageRepository.findByTenantIdIsNullAndLocale(locale);
        for (I18nMessage m : global) {
            result.put(m.getMessageKey(), m.getMessageValue());
        }
        if (tenantId != null) {
            List<I18nMessage> tenant = messageRepository.findByTenantIdAndLocale(tenantId, locale);
            for (I18nMessage m : tenant) {
                result.put(m.getMessageKey(), m.getMessageValue());
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency,
                              Long tenantId) {
        if (fromCurrency == null || toCurrency == null
                || fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }
        Optional<ExchangeRate> rate = findRate(tenantId, fromCurrency, toCurrency);
        if (rate.isEmpty()) {
            throw new NotFoundException(
                    "No exchange rate configured from " + fromCurrency + " to " + toCurrency);
        }
        return amount.multiply(rate.get().getRate()).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public ExchangeRate setRate(Long tenantId, String base, String target, BigDecimal rate) {
        Optional<ExchangeRate> existing = rateRepository
                .findByTenantIdOrTenantIdIsNull(tenantId)
                .stream()
                .filter(r -> r.getBaseCurrency().equalsIgnoreCase(base)
                        && r.getTargetCurrency().equalsIgnoreCase(target))
                .findFirst();
        if (existing.isPresent()) {
            existing.get().setRate(rate);
            return rateRepository.save(existing.get());
        }
        return rateRepository.save(new ExchangeRate(tenantId, base, target, rate));
    }

    @Transactional(readOnly = true)
    public List<ExchangeRate> rates(Long tenantId) {
        return rateRepository.findByTenantIdOrTenantIdIsNull(tenantId);
    }

    @Transactional(readOnly = true)
    public List<String> supportedCurrencies(Long tenantId) {
        Tenant t = requireTenant(tenantId);
        return splitList(t.getSupportedCurrencies(), t.getCurrencyIso());
    }

    @Transactional(readOnly = true)
    public List<String> supportedLanguages(Long tenantId) {
        Tenant t = requireTenant(tenantId);
        return splitList(t.getSupportedLanguages(), t.getDefaultLanguage());
    }

    @Transactional(readOnly = true)
    public Locale defaultLocale(Long tenantId) {
        Tenant t = requireTenant(tenantId);
        return parseLocale(t.getDefaultLanguage());
    }

    private Optional<ExchangeRate> findRate(Long tenantId, String from, String to) {
        Optional<ExchangeRate> rate;
        if (tenantId != null) {
            rate = rateRepository.findByTenantIdAndBaseCurrencyAndTargetCurrency(
                    tenantId, from.toUpperCase(), to.toUpperCase());
            if (rate.isPresent()) {
                return rate;
            }
        }
        rate = rateRepository.findByBaseCurrencyAndTargetCurrency(
                from.toUpperCase(), to.toUpperCase());
        if (rate.isPresent()) {
            return rate;
        }
        return rateRepository.findByBaseCurrencyAndTargetCurrency(
                to.toUpperCase(), from.toUpperCase())
                .map(r -> new ExchangeRate(null, from, to,
                        BigDecimal.ONE.divide(r.getRate(), 8, RoundingMode.HALF_UP)));
    }

    private List<String> splitList(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return List.of(fallback);
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private Locale parseLocale(String lang) {
        String[] parts = lang.split("[-_]");
        return parts.length == 2
                ? new Locale(parts[0], parts[1])
                : new Locale(parts[0]);
    }
}
