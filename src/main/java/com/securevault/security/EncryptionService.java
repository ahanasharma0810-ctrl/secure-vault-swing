package com.securevault.security;

import com.securevault.exception.CryptoException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Abstraction for any encryption algorithm.
 * The rest of the application only depends on this interface, so the
 * algorithm (AES today) can be swapped without changing other classes.
 */
public interface EncryptionService {

    /** Encrypts raw bytes. The result contains everything needed to decrypt it. */
    byte[] encrypt(byte[] plainData) throws CryptoException;

    /** Decrypts bytes produced by {@link #encrypt(byte[])}. */
    byte[] decrypt(byte[] encryptedData) throws CryptoException;

    /** Convenience method: encrypts text and returns Base64. */
    default String encryptText(String plainText) throws CryptoException {
        byte[] encrypted = encrypt(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /** Convenience method: decrypts Base64 text produced by {@link #encryptText(String)}. */
    default String decryptText(String encryptedText) throws CryptoException {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            return new String(decrypt(decoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new CryptoException("Encrypted text is not valid Base64", e);
        }
    }
}
