package com.ticketmesh.controller;

import com.ticketmesh.dto.ExchangeRateRequest;
import com.ticketmesh.dto.I18nMessageRequest;
import com.ticketmesh.model.ExchangeRate;
import com.ticketmesh.model.I18nMessage;
import com.ticketmesh.service.GlobalizationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Globalization: any tenant can translate UI strings, manage a locale
 * dictionary and configure exchange rates so money and text work anywhere.
 */
@RestController
@RequestMapping("/api/globalization")
public class GlobalizationController {

    private final GlobalizationService globalizationService;
    private final RequestContext requestContext;

    public GlobalizationController(GlobalizationService globalizationService,
                                   RequestContext requestContext) {
        this.globalizationService = globalizationService;
        this.requestContext = requestContext;
    }

    @GetMapping("/translate")
    public ResponseEntity<String> translate(
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam("locale") String locale,
            @RequestParam("key") String key) {
        return ResponseEntity.ok(
                globalizationService.resolve(requestContext.tenantIdOr(tenantId), locale, key));
    }

    @GetMapping("/dictionary")
    public ResponseEntity<Map<String, String>> dictionary(
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam("locale") String locale) {
        return ResponseEntity.ok(
                globalizationService.dictionary(requestContext.tenantIdOr(tenantId), locale));
    }

    @PostMapping("/messages")
    public ResponseEntity<I18nMessage> upsert(@RequestBody I18nMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(globalizationService.upsert(
                requestContext.tenantIdOr(request.tenantId()), request.locale(),
                request.key(), request.value()));
    }

    @PostMapping("/rates")
    public ResponseEntity<ExchangeRate> setRate(@RequestBody ExchangeRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(globalizationService.setRate(
                requestContext.tenantIdOr(request.tenantId()), request.base(),
                request.target(), request.rate()));
    }

    @GetMapping("/rates")
    public ResponseEntity<List<ExchangeRate>> rates(
            @RequestParam(value = "tenantId", required = false) Long tenantId) {
        return ResponseEntity.ok(globalizationService.rates(requestContext.tenantIdOr(tenantId)));
    }

    @GetMapping("/currencies")
    public ResponseEntity<List<String>> currencies(
            @RequestParam(value = "tenantId", required = false) Long tenantId) {
        return ResponseEntity.ok(globalizationService.supportedCurrencies(
                requestContext.tenantIdOr(tenantId)));
    }

    @GetMapping("/languages")
    public ResponseEntity<List<String>> languages(
            @RequestParam(value = "tenantId", required = false) Long tenantId) {
        return ResponseEntity.ok(globalizationService.supportedLanguages(
                requestContext.tenantIdOr(tenantId)));
    }
}