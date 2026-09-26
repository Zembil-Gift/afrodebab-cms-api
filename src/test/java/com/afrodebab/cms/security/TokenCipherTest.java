package com.afrodebab.cms.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenCipherTest {

    @Test
    void roundTripsToken() {
        TokenCipher cipher = new TokenCipher("some-secret-key");
        String token = "ATTA1234567890abcdef-trello-token";
        String encrypted = cipher.encrypt(token);
        assertNotEquals(token, encrypted);           // stored form is not plaintext
        assertEquals(token, cipher.decrypt(encrypted));
    }

    @Test
    void encryptionIsNonDeterministic() {
        TokenCipher cipher = new TokenCipher("k");
        assertNotEquals(cipher.encrypt("same"), cipher.encrypt("same")); // random IV per call
    }

    @Test
    void failsClosedWithoutKey() {
        TokenCipher cipher = new TokenCipher("");
        assertFalse(cipher.isConfigured());
        assertThrows(IllegalStateException.class, () -> cipher.encrypt("x"));
    }

    @Test
    void configuredWithKey() {
        assertTrue(new TokenCipher("key").isConfigured());
    }
}
