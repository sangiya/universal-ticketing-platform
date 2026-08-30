package com.ticketmesh.service;

import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.OtpCode;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.OtpCodeRepository;
import com.ticketmesh.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TwoFactorServiceTest {

    private UserRepository userRepository;
    private OtpCodeRepository otpCodeRepository;
    private NotificationService notificationService;
    private PasswordEncoder passwordEncoder;
    private TwoFactorService twoFactorService;

    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        otpCodeRepository = mock(OtpCodeRepository.class);
        notificationService = mock(NotificationService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        twoFactorService = new TwoFactorService(
                userRepository, otpCodeRepository, notificationService, passwordEncoder);

        user = new User("alice", "encoded", "Alice", "alice@example.com",
                User.Role.CUSTOMER, 1L, "0771234567");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "hash:" + inv.getArgument(0));
        when(passwordEncoder.matches(anyString(), anyString())).thenAnswer(inv -> {
            String raw = inv.getArgument(0);
            String hash = inv.getArgument(1);
            return hash.equals("hash:" + raw);
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(otpCodeRepository.save(any(OtpCode.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registrationOtp_sendsSavesEncryptedCodeAndNotifies() {
        twoFactorService.sendRegistrationOtp("alice@example.com");

        ArgumentCaptor<OtpCode> captor = ArgumentCaptor.forClass(OtpCode.class);
        verify(otpCodeRepository).save(captor.capture());
        OtpCode otp = captor.getValue();
        assertEquals(OtpCode.Purpose.REGISTRATION, otp.getPurpose());
        assertEquals(OtpCode.Channel.EMAIL, otp.getChannel());
        assertFalse(otp.isConsumed());
        assertTrue(otp.getCodeHash().startsWith("hash:"));
        verify(notificationService).notify(eq(1L), org.mockito.ArgumentMatchers.isNull(),
                eq(com.ticketmesh.model.Notification.Channel.EMAIL), anyString(), anyString());
    }

    @Test
    void registrationOtp_verifyCorrectCodeConsumesIt() {
        OtpCode otp = new OtpCode(user, OtpCode.Purpose.REGISTRATION, OtpCode.Channel.EMAIL,
                "hash:112233", Instant.now().plus(5, ChronoUnit.MINUTES));
        when(otpCodeRepository.findByUser_IdAndPurposeOrderByCreatedAtDesc(
                eq(user.getId()), eq(OtpCode.Purpose.REGISTRATION))).thenReturn(List.of(otp));

        boolean valid = twoFactorService.verifyRegistrationOtp("alice@example.com", "112233");

        assertTrue(valid);
        assertTrue(otp.isConsumed());
        verify(otpCodeRepository).save(otp);
    }

    @Test
    void registrationOtp_wrongCodeFails() {
        OtpCode otp = new OtpCode(user, OtpCode.Purpose.REGISTRATION, OtpCode.Channel.EMAIL,
                "hash:112233", Instant.now().plus(5, ChronoUnit.MINUTES));
        when(otpCodeRepository.findByUser_IdAndPurposeOrderByCreatedAtDesc(
                eq(user.getId()), eq(OtpCode.Purpose.REGISTRATION))).thenReturn(List.of(otp));

        boolean valid = twoFactorService.verifyRegistrationOtp("alice@example.com", "000000");

        assertFalse(valid);
        assertFalse(otp.isConsumed());
    }

    @Test
    void registrationOtp_expiredCodeFails() {
        OtpCode otp = new OtpCode(user, OtpCode.Purpose.REGISTRATION, OtpCode.Channel.EMAIL,
                "hash:112233", Instant.now().minus(1, ChronoUnit.MINUTES));
        when(otpCodeRepository.findByUser_IdAndPurposeOrderByCreatedAtDesc(
                eq(user.getId()), eq(OtpCode.Purpose.REGISTRATION))).thenReturn(List.of(otp));

        boolean valid = twoFactorService.verifyRegistrationOtp("alice@example.com", "112233");

        assertFalse(valid);
        assertFalse(otp.isConsumed());
    }

    @Test
    void registrationOtp_alreadyConsumedCodeFails() {
        OtpCode otp = new OtpCode(user, OtpCode.Purpose.REGISTRATION, OtpCode.Channel.EMAIL,
                "hash:112233", Instant.now().plus(5, ChronoUnit.MINUTES));
        otp.setConsumed(true);
        when(otpCodeRepository.findByUser_IdAndPurposeOrderByCreatedAtDesc(
                eq(user.getId()), eq(OtpCode.Purpose.REGISTRATION))).thenReturn(List.of(otp));

        assertFalse(twoFactorService.verifyRegistrationOtp("alice@example.com", "112233"));
    }

    @Test
    void registrationOtp_unknownEmailReturnsFalse() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertFalse(twoFactorService.verifyRegistrationOtp("ghost@example.com", "112233"));
    }

    @Test
    void totp_enableSetsSecretAndVerificationMatchesSameImplementation() {
        String secret = twoFactorService.enableTotp("alice");

        assertNotNull(secret);
        assertEquals(secret, user.getTotpSecret());
        assertTrue(user.isTotpEnabled());
        assertEquals(User.TwoFactorMethod.TOTP, user.getTwoFactorMethod());

        long nowSeconds = Instant.now().getEpochSecond();
        String code = TwoFactorService.generateCode(secret, nowSeconds);
        assertTrue(twoFactorService.verifyTotp("alice", code));
    }

    @Test
    void totp_rejectsWrongCode() {
        twoFactorService.enableTotp("alice");

        assertFalse(twoFactorService.verifyTotp("alice", "000000"));
    }

    @Test
    void totp_matchesRfc6238ReferenceVectors() {
        String secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
        assertEquals("287082", TwoFactorService.generateCode(secret, 59L));
        assertEquals("081804", TwoFactorService.generateCode(secret, 1_111_111_109L));
        assertEquals("005924", TwoFactorService.generateCode(secret, 1_234_567_890L));
        assertEquals("279037", TwoFactorService.generateCode(secret, 2_000_000_000L));
    }

    @Test
    void appKey_issueReturnsPlaintextKeyAndVerifyMatches() {
        String key = twoFactorService.issueAppKey("alice");

        assertNotNull(key);
        assertEquals(32, key.length());
        assertEquals("hash:" + key, user.getAppKeyHash());
        assertNotNull(user.getAppKeyIssuedAt());
        assertEquals(User.TwoFactorMethod.APP_KEY, user.getTwoFactorMethod());
        assertTrue(twoFactorService.verifyAppKey("alice", key));
        assertFalse(twoFactorService.verifyAppKey("alice", "definitely-a-wrong-key"));
    }

    @Test
    void appKey_unknownUsernameThrows() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(NotFoundException.class,
                () -> twoFactorService.issueAppKey("ghost"));
    }
}