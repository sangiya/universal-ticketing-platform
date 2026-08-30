package com.ticketmesh.service;

import java.io.ByteArrayOutputStream;

/**
 * RFC 4648 base32 (A-Z2-7), dependency-free. Used for TOTP shared secrets.
 */
final class Base32 {

    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    private Base32() {
    }

    static String encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int value = 0;
        int bits = 0;
        for (byte b : data) {
            value = (value << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                sb.append(ALPHABET[(value >>> (bits - 5)) & 0x1F]);
                bits -= 5;
            }
        }
        if (bits > 0) {
            sb.append(ALPHABET[(value << (5 - bits)) & 0x1F]);
        }
        return sb.toString();
    }

    static byte[] decode(String base32) {
        String input = base32.replace("=", "").toUpperCase();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int value = 0;
        int bits = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int idx = indexOf(c);
            if (idx < 0) {
                throw new IllegalArgumentException("Invalid base32 character: " + c);
            }
            value = (value << 5) | idx;
            bits += 5;
            if (bits >= 8) {
                out.write((value >>> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }

    private static int indexOf(char c) {
        for (int i = 0; i < ALPHABET.length; i++) {
            if (ALPHABET[i] == c) {
                return i;
            }
        }
        return -1;
    }
}