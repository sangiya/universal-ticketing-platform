package com.ticketmesh.service;

import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.Notification;
import com.ticketmesh.model.OtpCode;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.OtpCodeRepository;
import com.ticketmesh.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Email/SMS OTP, RFC 6238 TOTP and app-key two-factor auth. Fully offline and
 * dependency-free: no external TOTP library, only the JDK.
 */
@Service
public class TwoFactorService {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int OTP_LIFETIME_MINUTES = 5;
    private static final Duration OTP_LIFETIME = Duration.ofMinutes(OTP_LIFETIME_MINUTES);
    static final int TOTP_PERIOD_SECONDS = 30;
    static final int TOTP_DIGITS = 6;
    private static final int KEY_LENGTH = 32;

    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    public TwoFactorService(UserRepository userRepository,
                            OtpCodeRepository otpCodeRepository,
                            NotificationService notificationService,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public boolean sendRegistrationOtp(String email) {
        return sendOtp(email, OtpCode.Purpose.REGISTRATION);
    }

    @Transactional
    public boolean sendLoginOtp(String email) {
        return sendOtp(email, OtpCode.Purpose.LOGIN_2FA);
    }

    @Transactional
    public boolean sendOtp(String email, OtpCode.Purpose purpose) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No account found for email: " + email));
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        OtpCode otp = new OtpCode(
                user,
                purpose,
                OtpCode.Channel.EMAIL,
                passwordEncoder.encode(code),
                Instant.now().plus(OTP_LIFETIME));
        otpCodeRepository.save(otp);
        notificationService.notify(
                user.getTenantId(),
                user.getId(),
                Notification.Channel.EMAIL,
                "Your TicketMesh verification code",
                "Your verification code is " + code
                        + ". It expires in " + OTP_LIFETIME_MINUTES + " minutes.");
        log.info("Issued {} OTP for {}", purpose, email);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean verifyRegistrationOtp(String email, String code) {
        return verifyOtp(email, OtpCode.Purpose.REGISTRATION, code);
    }

    @Transactional
    public boolean verifyOtp(String email, OtpCode.Purpose purpose, String code) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || code == null || code.isBlank()) {
            return false;
        }
        List<OtpCode> candidates = otpCodeRepository
                .findByUser_IdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose);
        OtpCode otp = candidates.stream()
                .filter(o -> !o.isConsumed())
                .findFirst()
                .orElse(null);
        if (otp == null) {
            return false;
        }
        if (!otp.getExpiresAt().isAfter(Instant.now())) {
            return false;
        }
        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            return false;
        }
        otp.setConsumed(true);
        otpCodeRepository.save(otp);
        return true;
    }

    public String generateTotpSecret() {
        return Base32.encode(randomBytes(20));
    }

    @Transactional
    public String enableTotp(String username) {
        User user = requireByUsername(username);
        if (user.getTotpSecret() == null || user.getTotpSecret().isBlank()) {
            user.setTotpSecret(generateTotpSecret());
        }
        user.setTotpEnabled(true);
        user.setTwoFactorMethod(User.TwoFactorMethod.TOTP);
        userRepository.save(user);
        log.info("Enabled TOTP for {}", username);
        return user.getTotpSecret();
    }

    @Transactional(readOnly = true)
    public boolean verifyTotp(String username, String code) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !user.isTotpEnabled()
                || user.getTotpSecret() == null || code == null) {
            return false;
        }
        long nowSeconds = Instant.now().getEpochSecond();
        for (int window = -1; window <= 1; window++) {
            long candidate = (nowSeconds / TOTP_PERIOD_SECONDS + window) * TOTP_PERIOD_SECONDS;
            if (constantTimeEquals(generateCode(user.getTotpSecret(), candidate), code)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public String issueAppKey(String username) {
        User user = requireByUsername(username);
        String key = randomKey(KEY_LENGTH);
        user.setAppKeyHash(passwordEncoder.encode(key));
        user.setAppKeyIssuedAt(Instant.now());
        user.setTwoFactorMethod(User.TwoFactorMethod.APP_KEY);
        userRepository.save(user);
        log.info("Issued app key for {}", username);
        return key;
    }

    @Transactional(readOnly = true)
    public boolean verifyAppKey(String username, String key) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getAppKeyHash() == null || user.getAppKeyHash().isBlank()
                || key == null) {
            return false;
        }
        return passwordEncoder.matches(key, user.getAppKeyHash());
    }

    /**
     * RFC 6238 TOTP code for the given secret at the given Unix time (seconds).
     * Uses HMAC-SHA1, a 30s period and 6 digits as specified.
     */
    static String generateCode(String base32Secret, long unixSeconds) {
        byte[] key = Base32.decode(base32Secret);
        long counter = unixSeconds / TOTP_PERIOD_SECONDS;
        byte[] data = new byte[8];
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (counter & 0xFF);
            counter >>>= 8;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, TOTP_DIGITS);
            return String.format("%06d", otp);
        } catch (Exception ex) {
            throw new IllegalStateException("TOTP generation failed", ex);
        }
    }

    private User requireByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private String randomKey(int length) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}