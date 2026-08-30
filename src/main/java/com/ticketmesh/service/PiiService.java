package com.ticketmesh.service;

import com.ticketmesh.dto.PiiResponse;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.IdentityVerification;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.IdentityVerificationRepository;
import com.ticketmesh.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Protects sensitive PII at rest by writing encrypted copies of a user's email
 * and phone alongside the plaintext columns, and exposes only masked values to
 * callers. The document number in identity verifications is stored as
 * ciphertext by {@link IdentityVerificationService}; this service decrypts it
 * only long enough to produce a masked preview.
 */
@Service
public class PiiService {

    private final UserRepository userRepository;
    private final IdentityVerificationRepository identityRepository;
    private final PiiEncryptor piiEncryptor;

    public PiiService(UserRepository userRepository,
                      IdentityVerificationRepository identityRepository,
                      PiiEncryptor piiEncryptor) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.piiEncryptor = piiEncryptor;
    }

    /**
     * Ensures the encrypted at-rest copies of a user's email/phone are written.
     * Idempotent: any later call back-fills values that are missing.
     */
    @Transactional
    public User protect(User user) {
        boolean changed = false;
        if (user.getEmailEncrypted() == null) {
            user.setEmailEncrypted(piiEncryptor.encrypt(user.getEmail()));
            changed = true;
        }
        if (user.getPhoneEncrypted() == null && user.getPhone() != null) {
            user.setPhoneEncrypted(piiEncryptor.encrypt(user.getPhone()));
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }
        return user;
    }

    @Transactional(readOnly = true)
    public PiiResponse myPii(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        String emailMasked = mask(user.getEmail());
        String phoneMasked = mask(user.getPhone());
        String identityDocMasked = identityRepository.findByUser_Id(userId)
                .map(IdentityVerification::getDocumentNumber)
                .map(piiEncryptor::decrypt)
                .map(this::mask)
                .orElse(null);
        return new PiiResponse(emailMasked, phoneMasked, identityDocMasked, true);
    }

    private String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int keep = Math.min(2, value.length());
        return value.substring(0, keep) + "*".repeat(value.length() - keep);
    }
}
