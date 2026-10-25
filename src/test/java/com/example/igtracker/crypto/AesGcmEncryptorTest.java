package com.example.igtracker.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AesGcmEncryptorTest {
    @Test
    void roundTrip() {
        AesGcmEncryptor enc = new AesGcmEncryptor(AesGcmEncryptor.generateBase64Key());
        assertEquals("tajni-token-123", enc.decrypt(enc.encrypt("tajni-token-123")));
    }

    @Test
    void sameInputGivesDifferentCiphertextBecauseOfRandomIv() {
        AesGcmEncryptor enc = new AesGcmEncryptor(AesGcmEncryptor.generateBase64Key());
        assertNotEquals(enc.encrypt("x"), enc.encrypt("x"));
    }

    @Test
    void wrongKeyFailsAuthentication() {
        String cipher = new AesGcmEncryptor(AesGcmEncryptor.generateBase64Key()).encrypt("x");
        AesGcmEncryptor other = new AesGcmEncryptor(AesGcmEncryptor.generateBase64Key());
        assertThrows(IllegalStateException.class, () -> other.decrypt(cipher));
    }

    @Test
    void rejectsKeyOfWrongLength() {
        assertThrows(IllegalArgumentException.class, () -> new AesGcmEncryptor("AAAA"));
    }
}
