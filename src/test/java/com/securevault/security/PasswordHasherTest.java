package com.securevault.security;

import com.securevault.exception.CryptoException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher(1000);

    @Test
    void verifiesCorrectPassword() throws Exception {
        byte[] salt = hasher.generateSalt();
        byte[] hash = hasher.hash("Correct#1".toCharArray(), salt);
        assertTrue(hasher.verify("Correct#1".toCharArray(), salt, hash));
        assertFalse(hasher.verify("Wrong#1".toCharArray(), salt, hash));
    }

    @Test
    void saltsAreRandom() {
        assertFalse(java.util.Arrays.equals(hasher.generateSalt(), hasher.generateSalt()));
    }

    @Test
    void differentSaltGivesDifferentHash() throws Exception {
        char[] pw = "Same#Pass1".toCharArray();
        assertFalse(java.util.Arrays.equals(
                hasher.hash(pw, hasher.generateSalt()), hasher.hash(pw, hasher.generateSalt())));
    }

    @Test
    void keyIsTheSameForSamePasswordAndSalt() throws Exception {
        byte[] salt = hasher.generateSalt();
        assertArrayEquals(
                hasher.deriveKey("Pass#1234".toCharArray(), salt).getEncoded(),
                hasher.deriveKey("Pass#1234".toCharArray(), salt).getEncoded());
        assertEquals(32, hasher.deriveKey("Pass#1234".toCharArray(), salt).getEncoded().length);
    }

    @Test
    void emptyPasswordIsRejected() {
        assertThrows(CryptoException.class, () -> hasher.hash(new char[0], hasher.generateSalt()));
    }

    @Test
    void base64RoundTrip() throws Exception {
        byte[] data = {1, 2, 3, 127, -128};
        assertArrayEquals(data, PasswordHasher.fromBase64(PasswordHasher.toBase64(data)));
        assertThrows(CryptoException.class, () -> PasswordHasher.fromBase64("not base64!!"));
    }
}
