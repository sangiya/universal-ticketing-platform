package com.ticketmesh.service;

import com.ticketmesh.dto.IdentityResponse;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.IdentityVerification;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.IdentityVerificationRepository;
import com.ticketmesh.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent identity verification (NIC / passport / driving licence + photo).
 * Submission auto-approves in this offline build; a human/KYC would verify in
 * production. Admins can override through {@link #review}.
 */
@Service
public class IdentityVerificationService {

    private static final Logger log = LoggerFactory.getLogger(IdentityVerificationService.class);

    private final IdentityVerificationRepository identityRepository;
    private final UserRepository userRepository;
    private final PiiEncryptor piiEncryptor;

    public IdentityVerificationService(IdentityVerificationRepository identityRepository,
                                       UserRepository userRepository,
                                       PiiEncryptor piiEncryptor) {
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.piiEncryptor = piiEncryptor;
    }

    @Transactional
    public IdentityResponse submit(Long userId, String docType, String docNumber,
                                   String docPhotoUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        if (docNumber == null || docNumber.isBlank()) {
            throw new ConflictException("Document number is required");
        }
        IdentityVerification.DocumentType type;
        try {
            type = IdentityVerification.DocumentType.valueOf(docType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("Unsupported document type: " + docType
                    + " (expected NIC, PASSPORT or DRIVING_LICENCE)");
        }
        identityRepository.findByUser_Id(userId).ifPresent(identityRepository::delete);
        IdentityVerification verification = new IdentityVerification(
                user, type, piiEncryptor.encrypt(docNumber), docPhotoUrl);
        verification.markApproved(user.getId());
        IdentityVerification saved = identityRepository.save(verification);
        log.info("Identity verification {} submitted and auto-approved for user {}",
                saved.getId(), userId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public IdentityResponse byUser(Long userId) {
        IdentityVerification verification = identityRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException(
                        "No identity verification found for user " + userId));
        return toResponse(verification);
    }

    @Transactional
    public IdentityResponse review(Long verificationId, boolean approve, Long verifierId) {
        IdentityVerification verification = identityRepository.findById(verificationId)
                .orElseThrow(() -> new NotFoundException(
                        "Identity verification not found: " + verificationId));
        if (approve) {
            verification.markApproved(verifierId);
        } else {
            verification.markRejected();
        }
        IdentityVerification saved = identityRepository.save(verification);
        log.info("Identity verification {} {} by admin {}", saved.getId(),
                approve ? "approved" : "rejected", verifierId);
        return toResponse(saved);
    }

    private IdentityResponse toResponse(IdentityVerification v) {
        return new IdentityResponse(
                v.getId(),
                v.getUser().getId(),
                v.getDocumentType().name(),
                mask(v.getDocumentNumber()),
                v.getDocumentPhotoUrl(),
                v.getStatus().name(),
                v.getVerifiedBy(),
                v.getVerifiedAt(),
                v.getCreatedAt());
    }

    /**
     * Plaintext view of a verification document number for admin/KYC review.
     * The stored value is ciphertext; decrypt it for the reviewer.
     */
    @Transactional(readOnly = true)
    public String plaintextDocumentNumber(Long verificationId) {
        IdentityVerification verification = identityRepository.findById(verificationId)
                .orElseThrow(() -> new NotFoundException(
                        "Identity verification not found: " + verificationId));
        return piiEncryptor.decrypt(verification.getDocumentNumber());
    }

    private String mask(String docNumber) {
        String raw = piiEncryptor.decrypt(docNumber);
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        int keep = Math.min(2, raw.length());
        return raw.substring(0, keep) + "*".repeat(raw.length() - keep);
    }
}