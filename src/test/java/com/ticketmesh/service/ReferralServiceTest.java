package com.ticketmesh.service;

import com.ticketmesh.dto.ReferralResponse;
import com.ticketmesh.model.Referral;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.ReferralRepository;
import com.ticketmesh.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReferralServiceTest {

    private ReferralRepository referralRepository;
    private UserRepository userRepository;
    private PiiEncryptor piiEncryptor;
    private LoyaltyService loyaltyService;
    private ReferralService referralService;

    @BeforeEach
    void setUp() {
        referralRepository = mock(ReferralRepository.class);
        userRepository = mock(UserRepository.class);
        piiEncryptor = new PiiEncryptor("32-byte-key-0123456789abcdef01234567");
        loyaltyService = mock(LoyaltyService.class);
        referralService = new ReferralService(
                referralRepository, userRepository, piiEncryptor, loyaltyService);

        when(referralRepository.save(any(Referral.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(referralRepository.existsByCode(any(String.class))).thenReturn(false);
    }

    @Test
    void create_generatesUniqueCode() {
        ReferralResponse response = referralService.create(7L);

        assertNotNull(response.code());
        assertEquals(8, response.code().length());
        assertEquals("PENDING", response.status());
        assertEquals(0, response.rewardPoints());
        verify(referralRepository).existsByCode(eq(response.code()));
        verify(referralRepository).save(any(Referral.class));
    }

    @Test
    void invite_storesEncryptedEmail() {
        ReferralResponse response = referralService.invite(7L, "friend@example.com");

        assertEquals("PENDING", response.status());
        assertEquals("f*****@example.com", response.inviteeEmailMasked());
        verify(referralRepository).save(any(Referral.class));
    }

    @Test
    void validate_returnsTrueForPendingCode() {
        Referral referral = new Referral(7L, "ABC12345");
        when(referralRepository.findByCodeAndStatus("ABC12345", Referral.Status.PENDING))
                .thenReturn(Optional.of(referral));

        assertTrue(referralService.validate("ABC12345"));
    }

    @Test
    void validate_returnsFalseForMissingCode() {
        when(referralRepository.findByCodeAndStatus("MHQTY482", Referral.Status.PENDING))
                .thenReturn(Optional.empty());

        assertFalse(referralService.validate("MHQTY482"));
    }

    @Test
    void redeemOnJoin_rewardsReferrer() {
        Referral referral = new Referral(7L, "ABC12345");
        when(referralRepository.findByCodeAndStatus("ABC12345", Referral.Status.PENDING))
                .thenReturn(Optional.of(referral));
        User referrer = new User("referrer", "enc", "Referrer", "r@example.com",
                User.Role.CUSTOMER, 3L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(referrer));

        ReferralResponse response = referralService.redeemOnJoin(99L, "ABC12345");

        assertEquals(99L, response.inviteeUserId());
        assertEquals("REWARDED", response.status());
        assertEquals(100, response.rewardPoints());
        assertNotNull(response.joinedAt());
        verify(loyaltyService).earn(3L, 7L, 100L);
    }

    @Test
    void redeemOnJoin_rejectsUsedCode() {
        when(referralRepository.findByCodeAndStatus("USED1234", Referral.Status.PENDING))
                .thenReturn(Optional.empty());

        assertThrows(com.ticketmesh.exception.NotFoundException.class,
                () -> referralService.redeemOnJoin(99L, "USED1234"));
        verify(loyaltyService, org.mockito.Mockito.never())
                .earn(anyLong(), anyLong(), anyLong());
    }
}
