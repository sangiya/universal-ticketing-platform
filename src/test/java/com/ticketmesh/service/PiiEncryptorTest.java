package com.ticketmesh.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PiiEncryptorTest {

    @Test
    void encryptDecrypt_roundTrips() {
        PiiEncryptor encryptor = new PiiEncryptor("32-byte-key-0123456789abcdef01234567");

        String ciphertext = encryptor.encrypt("992310123V");

        assertEquals("992310123V", encryptor.decrypt(ciphertext));
    }

    @Test
    void encrypt_producesDifferentCiphertextEachTime() {
        PiiEncryptor encryptor = new PiiEncryptor("32-byte-key-0123456789abcdef01234567");

        String first = encryptor.encrypt("same-value");
        String second = encryptor.encrypt("same-value");

        assertNotEquals(first, second);
        assertEquals("same-value", encryptor.decrypt(first));
        assertEquals("same-value", encryptor.decrypt(second));
    }

    @Test
    void encryptDecrypt_nullSafe() {
        PiiEncryptor encryptor = new PiiEncryptor("32-byte-key-0123456789abcdef01234567");

        assertNull(encryptor.encrypt(null));
        assertNull(encryptor.decrypt(null));
    }

    @Test
    void decrypt_wrongKeyFails() {
        PiiEncryptor encryptor = new PiiEncryptor("32-byte-key-0123456789abcdef01234567");
        PiiEncryptor other = new PiiEncryptor("different-key-0123456789abcdef0123456");

        String ciphertext = encryptor.encrypt("secret");

        assertThrows(IllegalStateException.class, () -> other.decrypt(ciphertext));
    }
}
