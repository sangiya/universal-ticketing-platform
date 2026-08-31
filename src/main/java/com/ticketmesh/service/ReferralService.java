package com.ticketmesh.service;

import com.ticketmesh.dto.ReferralResponse;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.Referral;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.ReferralRepository;
import com.ticketmesh.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Referral / invite friends program. Each user can generate a unique invite
 * code and invite friends by email (the email is stored encrypted at rest).
 * When an invitee joins using a code, the referrer is rewarded loyalty points.
 */
@Service
public class ReferralService {

    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 8;
    private static final long REFERRAL_REWARD_POINTS = 100L;

    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;
    private final PiiEncryptor piiEncryptor;
    private final LoyaltyService loyaltyService;
    private final SecureRandom secureRandom = new SecureRandom();

    public ReferralService(ReferralRepository referralRepository,
                           UserRepository userRepository,
                           PiiEncryptor piiEncryptor,
                           LoyaltyService loyaltyService) {
        this.referralRepository = referralRepository;
        this.userRepository = userRepository;
        this.piiEncryptor = piiEncryptor;
        this.loyaltyService = loyaltyService;
    }

    @Transactional
    public ReferralResponse create(Long referrerUserId) {
        Referral referral = new Referral(referrerUserId, uniqueCode());
        return toResponse(referralRepository.save(referral));
    }

    @Transactional
    public ReferralResponse invite(Long referrerUserId, String inviteeEmail) {
        String code = uniqueCode();
        Referral referral = new Referral(referrerUserId, code);
        referral.setInviteeEmail(piiEncryptor.encrypt(inviteeEmail));
        return toResponse(referralRepository.save(referral));
    }

    @Transactional(readOnly = true)
    public List<ReferralResponse> myCodes(Long referrerUserId) {
        return referralRepository.findByReferrerUserId(referrerUserId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean validate(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return referralRepository.findByCodeAndStatus(code.trim().toUpperCase(),
                Referral.Status.PENDING).isPresent();
    }

    @Transactional
    public ReferralResponse redeemOnJoin(Long inviteeUserId, String code) {
        Referral referral = referralRepository
                .findByCodeAndStatus(code.trim().toUpperCase(), Referral.Status.PENDING)
                .orElseThrow(() -> new NotFoundException("Invalid referral code: " + code));
        if (referral.getReferrerUserId().equals(inviteeUserId)) {
            throw new ConflictException("Self-referral is not allowed");
        }
        if (referral.getStatus() != Referral.Status.PENDING) {
            throw new ConflictException("Referral code has already been used");
        }
        referral.markJoined(inviteeUserId);
        referral.setStatus(Referral.Status.REWARDED);
        referral.setRewardPoints(REFERRAL_REWARD_POINTS);
        RewardPoints(referral);
        return toResponse(referralRepository.save(referral));
    }

    private void RewardPoints(Referral referral) {
        User referrer = userRepository.findById(referral.getReferrerUserId())
                .orElseThrow(() -> new NotFoundException(
                        "Referrer not found: " + referral.getReferrerUserId()));
        Long tenantId = referrer.getTenantId() != null ? referrer.getTenantId() : 1L;
        loyaltyService.earn(tenantId, referral.getReferrerUserId(), REFERRAL_REWARD_POINTS);
    }

    private String uniqueCode() {
        String code;
        do {
            code = generateCode();
        } while (referralRepository.existsByCode(code));
        return code;
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)]);
        }
        return sb.toString();
    }

    private ReferralResponse toResponse(Referral referral) {
        String inviteeEmail = referral.getInviteeEmail() == null
                ? null
                : maskEmail(piiEncryptor.decrypt(referral.getInviteeEmail()));
        return new ReferralResponse(
                referral.getId(),
                referral.getCode(),
                referral.getStatus().name(),
                inviteeEmail,
                referral.getInviteeUserId(),
                referral.getRewardPoints(),
                referral.getCreatedAt(),
                referral.getJoinedAt());
    }

    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        return local.substring(0, 1) + "*".repeat(Math.max(1, local.length() - 1)) + domain;
    }
}
