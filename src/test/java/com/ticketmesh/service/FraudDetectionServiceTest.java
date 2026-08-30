package com.ticketmesh.service;

import com.ticketmesh.dto.FraudCheckRequest;
import com.ticketmesh.dto.FraudCheckResponse;
import com.ticketmesh.repository.FraudSignalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class FraudDetectionServiceTest {

    private FraudSignalRepository signalRepository;
    private FraudDetectionService fraudDetectionService;

    @BeforeEach
    void setUp() {
        signalRepository = mock(FraudSignalRepository.class);
        fraudDetectionService = new FraudDetectionService(signalRepository);
    }

    @Test
    void evaluate_lowRiskClearsTransactionWithoutPersisting() {
        FraudCheckRequest request = cleanRequest();

        FraudCheckResponse response = fraudDetectionService.evaluate(request);

        assertEquals("LOW", response.risk());
        assertFalse(response.blocked());
        assertEquals(0, response.score());
        verify(signalRepository, never()).save(any());
    }

    @Test
    void evaluate_highVelocityAndBulkPurchaseBlocksAndPersists() {
        FraudCheckRequest request = cleanRequest();
        request.setAmount(new BigDecimal("7500.00"));
        request.setQuantity(30);
        request.setAttemptsInWindow(12);
        request.setDistinctCardsInWindow(7);

        FraudCheckResponse response = fraudDetectionService.evaluate(request);

        assertTrue(response.blocked());
        assertEquals("HIGH", response.risk());
        assertTrue(response.flags().contains("HIGH_VELOCITY"));
        assertTrue(response.flags().contains("CARD_TESTING"));
        assertTrue(response.flags().contains("BULK_PURCHASE"));
        assertTrue(response.flags().contains("HIGH_VALUE"));
        assertTrue(response.flags().contains("SCALPING_PATTERN"));
        verify(signalRepository).save(any());
    }

    @Test
    void evaluate_mediumRiskPersistsButDoesNotBlock() {
        FraudCheckRequest request = cleanRequest();
        request.setDistinctCardsInWindow(7);
        request.setAmount(new BigDecimal("6000.00"));

        FraudCheckResponse response = fraudDetectionService.evaluate(request);

        assertEquals("MEDIUM", response.risk());
        assertFalse(response.blocked());
        assertTrue(response.flags().contains("CARD_TESTING"));
        verify(signalRepository).save(any());
    }

    @Test
    void evaluate_adminOverrideAlwaysClears() {
        FraudCheckRequest request = cleanRequest();
        request.setAmount(new BigDecimal("99999.00"));
        request.setQuantity(50);
        request.setAttemptsInWindow(99);
        request.setDistinctCardsInWindow(99);
        request.setAdminOverride(true);

        FraudCheckResponse response = fraudDetectionService.evaluate(request);

        assertEquals("LOW", response.risk());
        assertFalse(response.blocked());
        verify(signalRepository, never()).save(any());
    }

    private FraudCheckRequest cleanRequest() {
        FraudCheckRequest request = new FraudCheckRequest();
        request.setTenantId(1L);
        request.setActorUsername("alice");
        request.setSubjectType("BOOKING");
        request.setSubjectRef("ticketmesh-ABC");
        request.setAmount(new BigDecimal("1200.00"));
        request.setQuantity(1);
        request.setAttemptsInWindow(1);
        request.setDistinctCardsInWindow(1);
        return request;
    }
}
