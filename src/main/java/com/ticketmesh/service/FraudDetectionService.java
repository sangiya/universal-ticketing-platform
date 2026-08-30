package com.ticketmesh.service;

import com.ticketmesh.dto.FraudCheckRequest;
import com.ticketmesh.dto.FraudCheckResponse;
import com.ticketmesh.model.FraudSignal;
import com.ticketmesh.model.FraudSignal.Risk;
import com.ticketmesh.repository.FraudSignalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Risk/fraud scoring for booking and payment transactions. Produces a 0-100
 * score from transparent heuristics (velocity, quantity, high value, repeated
 * attempts), persists a {@link FraudSignal} for medium/high risk, and can block
 * the transaction. This powers the platform's auto issue/fraud detection.
 */
@Service
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);
    private static final int BLOCK_THRESHOLD = 75;

    private final FraudSignalRepository signalRepository;

    public FraudDetectionService(FraudSignalRepository signalRepository) {
        this.signalRepository = signalRepository;
    }

    @Transactional
    public FraudCheckResponse evaluate(FraudCheckRequest request) {
        if (request.isAdminOverride()) {
            return new FraudCheckResponse(0, Risk.LOW.name(), Set.of(), false);
        }

        int score = 0;
        Set<String> flags = new LinkedHashSet<>();

        if (request.getAttemptsInWindow() > 10) {
            score += 25;
            flags.add("HIGH_VELOCITY");
        } else if (request.getAttemptsInWindow() > 5) {
            score += 10;
            flags.add("ELEVATED_VELOCITY");
        }

        if (request.getDistinctCardsInWindow() > 5) {
            score += 25;
            flags.add("CARD_TESTING");
        }

        if (request.getQuantity() > 20) {
            score += 20;
            flags.add("BULK_PURCHASE");
        }

        if (request.getAmount() != null && request.getAmount().compareTo(new BigDecimal("5000")) > 0) {
            score += 15;
            flags.add("HIGH_VALUE");
        }

        if (request.getQuantity() > 10 && request.getAttemptsInWindow() > 5) {
            score += 15;
            flags.add("SCALPING_PATTERN");
        }

        score = Math.min(100, score);
        Risk risk = score >= BLOCK_THRESHOLD ? Risk.HIGH
                : score >= 40 ? Risk.MEDIUM : Risk.LOW;
        boolean blocked = risk == Risk.HIGH;

        if (risk != Risk.LOW) {
            FraudSignal signal = new FraudSignal(
                    request.getTenantId(), request.getSubjectType(), request.getSubjectRef(),
                    request.getActorUsername(), score, risk, String.join(",", flags),
                    "Score " + score + " via " + flags);
            signalRepository.save(signal);
            log.warn("Fraud signal {} ({}): {} for {}",
                    request.getSubjectRef(), risk, score, request.getActorUsername());
        }

        return new FraudCheckResponse(score, risk.name(), flags, blocked);
    }

    @Transactional(readOnly = true)
    public List<FraudSignal> recent(Long tenantId) {
        return signalRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public long countHighRisk() {
        return signalRepository.countByRisk(Risk.HIGH);
    }
}
