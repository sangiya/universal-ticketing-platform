package com.ticketmesh.dto;

/**
 * Masked view of an authenticated user's sensitive PII. Raw values are never
 * returned; only masked forms plus a flag indicating at-rest protection is
 * active.
 */
public record PiiResponse(String emailMasked, String phoneMasked,
                          String identityDocMasked, boolean piiProtected) {
}
