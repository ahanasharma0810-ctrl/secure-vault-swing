package com.securevault.security;

import com.securevault.config.AppConfig;
import com.securevault.exception.CryptoException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Turns the master password into
 *  1. a hash, used to check the password at login, and
 *  2. an AES key, used to encrypt the vault.
 *
 * Uses PBKDF2 with HMAC-SHA256 and a random salt, so the same password
 * never gives the same hash twice and brute-force attacks are slow.
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final int iterations;

    public PasswordHasher() {
        this(AppConfig.PBKDF2_ITERATIONS);
    }

    /** Lets tests use fewer iterations so they run fast. */
    public PasswordHasher(int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException("Iterations must be positive");
        }
        this.iterations = iterations;
    }

    /** Creates a new random salt. */
    public byte[] generateSalt() {
        byte[] salt = new byte[AppConfig.SALT_LENGTH_BYTES];
        RANDOM.nextBytes(salt);
        return salt;
    }

    /** Hashes the password with the given salt. */
    public byte[] hash(char[] password, byte[] salt) throws CryptoException {
        return pbkdf2(password, salt);
    }

    /** Checks a password against a stored hash in constant time. */
    public boolean verify(char[] password, byte[] salt, byte[] expectedHash) throws CryptoException {
        byte[] actual = pbkdf2(password, salt);
        return MessageDigest.isEqual(actual, expectedHash);
    }

    /** Derives the AES-256 key used to encrypt the vault. */
    public SecretKey deriveKey(char[] password, byte[] salt) throws CryptoException {
        byte[] keyBytes = pbkdf2(password, salt);
        return new SecretKeySpec(keyBytes, "AES");
    }

    private byte[] pbkdf2(char[] password, byte[] salt) throws CryptoException {
        if (password == null || password.length == 0) {
            throw new CryptoException("Password cannot be empty");
        }
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, AppConfig.KEY_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Could not hash the password", e);
        } finally {
            spec.clearPassword();
        }
    }

    // ---------- Base64 helpers for saving bytes as text ----------
    public static String toBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] fromBase64(String text) throws CryptoException {
        try {
            return Base64.getDecoder().decode(text);
        } catch (IllegalArgumentException e) {
            throw new CryptoException("Stored value is not valid Base64", e);
        }
    }
}
