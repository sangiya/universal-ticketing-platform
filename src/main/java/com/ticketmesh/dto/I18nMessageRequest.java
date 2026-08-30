package com.ticketmesh.dto;

public record I18nMessageRequest(Long tenantId, String locale, String key, String value) {
}