package com.ticketmesh.service;

import com.ticketmesh.dto.SavedTravelerRequest;
import com.ticketmesh.dto.SavedTravelerResponse;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.SavedTraveler;
import com.ticketmesh.repository.SavedTravelerRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TravelerService {

    private final SavedTravelerRepository travelerRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final PiiEncryptor encryptor;

    public TravelerService(SavedTravelerRepository travelerRepository,
                           UserRepository userRepository,
                           CurrentUser currentUser,
                           PiiEncryptor encryptor) {
        this.travelerRepository = travelerRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.encryptor = encryptor;
    }

    @Transactional
    public SavedTravelerResponse create(SavedTravelerRequest req) {
        var user = userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        if (req.consentGiven() == null || !req.consentGiven()) {
            throw new ConflictException("Consent is required to store traveler data");
        }
        if (req.fullName() == null || req.fullName().isBlank()) {
            throw new ConflictException("Full name is required");
        }
        var entity = new SavedTraveler(
                user.getTenantId() != null ? user.getTenantId() : 1L,
                user.getId(),
                req.fullName().trim(),
                parseRel(req.relationship()),
                req.dateOfBirth(),
                parseGender(req.gender()),
                req.nationality(),
                parseDoc(req.documentType()),
                encryptor.encrypt(req.documentNumber()),
                encryptor.encrypt(req.phone()),
                encryptor.encrypt(req.email()),
                true
        );
        travelerRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<SavedTravelerResponse> listMine() {
        var user = userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        return travelerRepository.findByOwnerUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        var user = userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        var t = travelerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Traveler not found: " + id));
        if (!t.getOwnerUserId().equals(user.getId())) {
            throw new NotFoundException("Traveler not found: " + id);
        }
        // historical bookings remain — we only delete the saved profile
        travelerRepository.delete(t);
    }

    @Transactional(readOnly = true)
    public List<SavedTravelerResponse> export() {
        return listMine();
    }

    private SavedTravelerResponse toResponse(SavedTraveler t) {
        return new SavedTravelerResponse(
                t.getId(), t.getFullName(), t.getRelationship().name(),
                t.getDateOfBirth(), t.getGender().name(), t.getNationality(),
                t.getDocumentType() != null ? t.getDocumentType().name() : null,
                mask(t.getDocumentNumberEnc(), 4),
                mask(t.getPhoneEnc(), 4),
                maskEmail(t.getEmailEnc()),
                t.isConsentGiven(), t.getConsentAt(), t.getCreatedAt()
        );
    }

    private String mask(String enc, int keep) {
        if (enc == null) return null;
        try {
            String plain = encryptor.decrypt(enc);
            if (plain == null || plain.length() <= keep) return "***";
            return "*".repeat(plain.length() - keep) + plain.substring(plain.length() - keep);
        } catch (Exception e) { return "***"; }
    }

    private String maskEmail(String enc) {
        if (enc == null) return null;
        try {
            String plain = encryptor.decrypt(enc);
            if (plain == null || !plain.contains("@")) return mask(enc, 4);
            int at = plain.indexOf('@');
            String local = plain.substring(0, at);
            String domain = plain.substring(at);
            String maskedLocal = local.length() <=1 ? "*" : local.charAt(0) + "*".repeat(local.length()-1);
            return maskedLocal + domain;
        } catch (Exception e) { return "***"; }
    }

    private SavedTraveler.Relationship parseRel(String v) {
        if (v == null || v.isBlank()) return SavedTraveler.Relationship.OTHER;
        try { return SavedTraveler.Relationship.valueOf(v.trim().toUpperCase()); }
        catch (Exception e) { return SavedTraveler.Relationship.OTHER; }
    }
    private SavedTraveler.Gender parseGender(String v) {
        if (v == null || v.isBlank()) return SavedTraveler.Gender.UNSPECIFIED;
        try { return SavedTraveler.Gender.valueOf(v.trim().toUpperCase()); }
        catch (Exception e) { return SavedTraveler.Gender.UNSPECIFIED; }
    }
    private SavedTraveler.DocumentType parseDoc(String v) {
        if (v == null || v.isBlank()) return null;
        try { return SavedTraveler.DocumentType.valueOf(v.trim().toUpperCase()); }
        catch (Exception e) { return SavedTraveler.DocumentType.OTHER; }
    }
}
