package com.ticketmesh.service;

import com.ticketmesh.dto.IdentityResponse;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.IdentityVerification;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.IdentityVerificationRepository;
import com.ticketmesh.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityVerificationServiceTest {

    private IdentityVerificationRepository identityRepository;
    private UserRepository userRepository;
    private PiiEncryptor piiEncryptor;
    private IdentityVerificationService identityService;

    private User user;

    @BeforeEach
    void setUp() {
        identityRepository = mock(IdentityVerificationRepository.class);
        userRepository = mock(UserRepository.class);
        piiEncryptor = new PiiEncryptor("test-pii-key-0123456789abcdef0123456789abcdef");
        identityService = new IdentityVerificationService(
                identityRepository, userRepository, piiEncryptor);

        user = new User("agent", "encoded", "Agent One", "agent@example.com",
                User.Role.AGENT, 5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(identityRepository.save(any(IdentityVerification.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void submit_createsApprovedVerification() {
        IdentityResponse response = identityService.submit(
                5L, "NIC", "992310123V", "https://cdn.example.test/nic.jpg");

        assertEquals("APPROVED", response.status());
        assertEquals("NIC", response.documentType());
        assertEquals("99********", response.documentNumberMasked());
        verify(identityRepository).save(any(IdentityVerification.class));
    }

    @Test
    void submit_rejectsBadDocumentType() {
        assertThrows(ConflictException.class,
                () -> identityService.submit(5L, "TRIBAL_TATTOO", "123", null));
        verify(identityRepository, never()).save(any(IdentityVerification.class));
    }

    @Test
    void submit_replacesExistingVerification() {
        IdentityVerification existing = new IdentityVerification(
                user, IdentityVerification.DocumentType.NIC, "992310123V", null);
        when(identityRepository.findByUser_Id(5L)).thenReturn(Optional.of(existing));

        identityService.submit(5L, "PASSPORT", "N1234567", null);

        verify(identityRepository).delete(existing);
        verify(identityRepository).save(any(IdentityVerification.class));
    }

    @Test
    void byUser_returnsVerification() {
        IdentityVerification verification = new IdentityVerification(
                user, IdentityVerification.DocumentType.DRIVING_LICENCE,
                piiEncryptor.encrypt("B-122334"), null);
        when(identityRepository.findByUser_Id(5L)).thenReturn(Optional.of(verification));

        IdentityResponse response = identityService.byUser(5L);

        assertEquals("DRIVING_LICENCE", response.documentType());
        assertEquals("B-******", response.documentNumberMasked());
    }

    @Test
    void byUser_throwsWhenMissing() {
        when(identityRepository.findByUser_Id(5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> identityService.byUser(5L));
    }

    @Test
    void review_approvesAndRejects() {
        IdentityVerification verification = new IdentityVerification(
                user, IdentityVerification.DocumentType.PASSPORT,
                piiEncryptor.encrypt("N1234567"), null);
        when(identityRepository.findById(99L)).thenReturn(Optional.of(verification));

        IdentityResponse approved = identityService.review(99L, true, 7L);
        assertEquals("APPROVED", approved.status());
        assertEquals(7L, approved.verifiedBy());

        IdentityResponse rejected = identityService.review(99L, false, 7L);
        assertEquals("REJECTED", rejected.status());
        verify(identityRepository, org.mockito.Mockito.times(2))
                .save(verification);
    }
}