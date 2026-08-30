package com.ticketmesh.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256/GCM encryptor used to protect sensitive PII at rest (identity
 * document numbers, referral invitee emails and user contact copies). A
 * random 12-byte IV is generated per encryption and stored alongside the
 * ciphertext, both base64-encoded. Null-safe: encrypt(null) and
 * decrypt(null) both return null.
 */
@Component
public class PiiEncryptor {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;

    public PiiEncryptor(@Value("${app.pii.master-key:ticketmesh-pii-master-key-change-me-2026!!}")
                        String masterKey) {
        this.key = new SecretKeySpec(padKey(masterKey), "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt PII value", ex);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] payload = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] plaintext = cipher.doFinal(payload);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt PII value", ex);
        }
    }

    private static byte[] padKey(String masterKey) {
        byte[] raw = masterKey.getBytes(StandardCharsets.UTF_8);
        byte[] padded = new byte[32];
        System.arraycopy(raw, 0, padded, 0, Math.min(raw.length, padded.length));
        return padded;
    }
}
